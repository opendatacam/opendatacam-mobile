/*
 * Adapting : https://github.com/nihui/ncnn-android-yolov5/blob/master/app/src/main/jni/yolov5ncnn_jni.cpp
 *
 * To work with android
 *
 */

#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>
#include <ncnn/net.h>

static ncnn::UnlockedPoolAllocator g_blob_pool_allocator;
static ncnn::PoolAllocator g_workspace_pool_allocator;

typedef struct Bbox {
    float x1;
    float y1;
    float x2;
    float y2;
    float score;
    int label;
} Bbox;

static ncnn::Net yolov4;
static int input_size_w;
static int input_size_h;
static bool useGpu = false;
static std::vector<Bbox> objects;

static jclass bboxCls = NULL;
static jmethodID constructorId;
static jfieldID xId;
static jfieldID yId;
static jfieldID wId;
static jfieldID hId;
static jfieldID labelId;
static jfieldID probId;

extern "C" JNIEXPORT int JNICALL
Java_com_opendatacam_YOLOv4_init(JNIEnv *env, jclass clazz, jobject assetManager, jboolean use_gpu) {

    if(ncnn::get_gpu_count() > 0) {
        useGpu = use_gpu;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    input_size_w = 320;
    input_size_h = 192;

    yolov4.opt.use_vulkan_compute = useGpu;
    yolov4.opt.use_fp16_arithmetic = true;
    yolov4.opt.lightmode = true;
    yolov4.opt.num_threads = 4;
    yolov4.opt.blob_allocator = &g_blob_pool_allocator;
    yolov4.opt.workspace_allocator = &g_workspace_pool_allocator;
    yolov4.opt.use_packing_layout = true;
    yolov4.load_param(mgr, "yolov4-tiny-opt.param");
    yolov4.load_model(mgr,  "yolov4-tiny-opt.bin");

    // init jni glue
    jclass localBboxCls = env->FindClass("com/opendatacam/Bbox");
    bboxCls = reinterpret_cast<jclass>(env->NewGlobalRef(localBboxCls));

    constructorId = env->GetMethodID(bboxCls, "<init>", "(FFFFIF)V");

    return JNI_TRUE;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_opendatacam_YOLOv4_detect(JNIEnv *env, jclass thiz, jobject bitmap, jdouble threshold,
                                   jdouble nms_threshold) {

    AndroidBitmapInfo img_size;
    AndroidBitmap_getInfo(env, bitmap, &img_size);
    ncnn::Mat in = ncnn::Mat::from_android_bitmap_resize(env, bitmap, ncnn::Mat::PIXEL_RGBA2RGB, input_size_w,
                                                             input_size_h);

    const float mean_vals[3] = {0, 0, 0};
    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    in.substract_mean_normalize(mean_vals, norm_vals);

    ncnn::Extractor ex = yolov4.create_extractor();

    ex.input("data", in);

    ncnn::Mat out;
    ex.extract("output", out);

    objects.clear();
    for (int i = 0; i < out.h; i++)
    {
        const float* values = out.row(i);

        Bbox bbox;
        bbox.label = values[0] - 1;
        bbox.score = values[1];
        bbox.x1 = values[2];
        bbox.y1 = values[3];
        bbox.x2 = values[4];
        bbox.y2 = values[5];

        objects.push_back(bbox);
    }

    jobjectArray jObjArray = env->NewObjectArray(objects.size(), bboxCls, NULL);

    for (size_t i=0; i<objects.size(); i++)
    {
        jobject jObj = env->NewObject(bboxCls, constructorId, objects[i].x1, objects[i].y1, objects[i].x2, objects[i].y2, objects[i].label, objects[i].score);
        env->SetObjectArrayElement(jObjArray, i, jObj);
    }

    return jObjArray;
}
