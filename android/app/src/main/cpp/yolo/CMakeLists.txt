cmake_minimum_required(VERSION 3.4.1)

add_library( # Sets the name of the library.
        yolov4

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        yolov4_jni_interface.cpp
        )

include_directories(
        ../ncnnvulkan/include
)
# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library(log-lib log)
find_library(android-lib android)
find_library(vulkan-lib vulkan)
find_library(jnigraphics-lib jnigraphics)
add_library( ncnn STATIC IMPORTED )
set_target_properties( # Specifies the target library.
        ncnn

        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION

        # Provides the path to the library you want to import.
        ${CMAKE_SOURCE_DIR}/ncnnvulkan/${ANDROID_ABI}/libncnn.a )


# ncnnvulkan
add_library(glslang STATIC IMPORTED)
add_library(OGLCompiler STATIC IMPORTED)
add_library(OSDependent STATIC IMPORTED)
add_library(SPIRV STATIC IMPORTED)
set_target_properties(glslang PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/ncnnvulkan/${ANDROID_ABI}/libglslang.a)
set_target_properties(OGLCompiler PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/ncnnvulkan/${ANDROID_ABI}/libOGLCompiler.a)
set_target_properties(OSDependent PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/ncnnvulkan/${ANDROID_ABI}/libOSDependent.a)
set_target_properties(SPIRV PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/ncnnvulkan/${ANDROID_ABI}/libSPIRV.a)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries( # Specifies the target library.
        yolov4
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        ${vulkan-lib}
        ${android-lib}
        ${jnigraphics-lib}
        ncnn
        glslang SPIRV OGLCompiler OSDependent )