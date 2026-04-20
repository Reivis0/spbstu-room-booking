#ifndef PARSER_FACTORY_HPP
#define PARSER_FACTORY_HPP

#include "university_parser.hpp"
#include "spbptu_parser.hpp"
#include "spbgu_parser.hpp"
#include "leti_parser.hpp"
#include <stdexcept>

class ParserFactory {
public:
    static std::unique_ptr<UniversityParser> create(const std::string& university_id) {
        if (university_id == "spbptu") return std::make_unique<SpbptuParser>();
        if (university_id == "spbgu") return std::make_unique<SpbguParser>();
        if (university_id == "leti") return std::make_unique<LetiParser>();
        throw std::runtime_error("Unknown university_id: " + university_id);
    }
};

#endif
