cmake_minimum_required(VERSION 3.22.1)
project(qidk_backend)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable exception handling and RTTI
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions -frtti")

add_library(qidk_backend SHARED
    qidk_backend.cpp
)

# Placeholder: add QNN include directories when available
# target_include_directories(qidk_backend PRIVATE ${QNN_SDK_ROOT}/include)

# Placeholder: link QNN libs (e.g., QNN_LIBS) after copying into jniLibs
# target_link_libraries(qidk_backend
#     log
# )

target_link_libraries(qidk_backend
    log
)
