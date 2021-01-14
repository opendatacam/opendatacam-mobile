cmake_minimum_required(VERSION 3.4.1)

add_library( # Sets the name of the library.
        nodejsmobile

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        nodejs-mobile.cpp)

include_directories(../libnode/include/node/)

add_library( libnode
        SHARED
        IMPORTED )
set_target_properties( # Specifies the target library.
        libnode
        # Specifies the parameter you want to define.
        PROPERTIES IMPORTED_LOCATION
        # Provides the path to the library you want to import.
        ${CMAKE_SOURCE_DIR}/libnode/bin/${ANDROID_ABI}/libnode.so )

find_library(log-lib log)

target_link_libraries( # Specifies the target library.
        nodejsmobile
        libnode
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})