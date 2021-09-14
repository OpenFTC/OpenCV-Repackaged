cmake_minimum_required(VERSION 3.6)

# dummy target to bring libc++_shared.so into packages
add_library(dummy_target_cpp_lib STATIC dummy.cpp)
