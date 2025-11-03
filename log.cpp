#include <memory>
#include <string>
#include <iostream>

// Header:
struct SomeInterface {
    int operation() const;
    SomeInterface();
    SomeInterface(SomeInterface&&) = default;
    ~SomeInterface();
private:    
    struct Implementation;
    std::unique_ptr<Implementation> pimpl_;   
};

// User only depends on SomeInterface, implementations behind
// SomeInterface can be swapped without affecting user.
void user(SomeInterface data) {
    int v = data.operation();
    std::cout << v << "\n";
}

// Remove the -DIMPL_A flag to switch to the other implementation
#ifdef IMPL_A

// Implementation A
struct SomeInterface::Implementation {
    int operation() { return rank; }
    int rank;
};

SomeInterface::~SomeInterface() = default;
SomeInterface::SomeInterface()
  : pimpl_(std::make_unique<SomeInterface::Implementation>(10)) {}

int SomeInterface::operation() const {
    return pimpl_->operation();
}

#else

// Implementation B
struct SomeInterface::Implementation {
    int operation() { return std::stoi(text); }
    std::string text;
};

SomeInterface::~SomeInterface() = default;
SomeInterface::SomeInterface() 
  : pimpl_(std::make_unique<SomeInterface::Implementation>("42")) {}

int SomeInterface::operation() const {
    return pimpl_->operation();
}

#endif

int main() {
    SomeInterface iface;
    user(std::move(iface));
}