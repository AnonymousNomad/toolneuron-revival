cmake_minimum_required(VERSION 3.22.1)
project("terminalpty")

add_library(${CMAKE_PROJECT_NAME} SHARED
    terminal_pty.c
)

target_compile_options(${CMAKE_PROJECT_NAME} PRIVATE
    -Wall
    -fstack-protector-strong
    -fvisibility=hidden
    $<$<NOT:$<CONFIG:Debug>>:-O2>
)

target_link_libraries(${CMAKE_PROJECT_NAME}
    log
)

target_link_options(${CMAKE_PROJECT_NAME} PRIVATE
    -Wl,-z,max-page-size=16384
)
