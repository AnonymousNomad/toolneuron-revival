#include "collection.h"
#include <cassert>
#include <filesystem>
#include <fstream>
#include <vector>
int main(int argc, char** argv) {
    assert(argc == 2);
    const std::string base = argv[1];
    std::filesystem::create_directories(base);
    const std::vector<std::vector<unsigned char>> corrupt = {{1}, {10, 0, 0, 0, 1}, {0, 0, 0, 0}, {3, 0, 0, 0, 1, 2, 3}};
    for (const auto& bytes : corrupt) {
        const std::string path = base + "/state.hxs";
        { std::ofstream out(path, std::ios::binary | std::ios::trunc); out.write(reinterpret_cast<const char*>(bytes.data()), bytes.size()); }
        { hxs::Collection collection("state", base, {}); assert(!collection.load()); }
        assert(std::filesystem::file_size(path) == bytes.size());
    }
    std::filesystem::remove(base + "/state.hxs");
    { std::ofstream out(base + "/state.wal", std::ios::binary); out << "partial"; }
    { hxs::Collection collection("state", base, {}); assert(!collection.load()); }
    assert(std::filesystem::file_size(base + "/state.wal") == 7);
}
