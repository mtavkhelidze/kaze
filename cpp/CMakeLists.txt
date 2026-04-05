cmake_minimum_required(VERSION 4.3)
project(LibKaze)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

set(GENERATED_FILE ${CMAKE_CURRENT_SOURCE_DIR}/src/kaze.cpp)

add_library(kaze SHARED src/utils.cpp)
set(CMAKE_SHARED_LIBRARY_PREFIX "lib")
if(EXISTS ${GENERATED_FILE})
    target_sources(kaze PRIVATE ${GENERATED_FILE})
endif()

add_executable(kaze-test src/main.cpp src/utils.cpp)
