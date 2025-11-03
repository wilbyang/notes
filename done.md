The std::equal_range algorithm is a binary search algorithm (2*logn comparisons) that operates on sorted ranges.

It returns the lower and upper bounds for the given value, denoting the range of elements that equal the value.

Same as std::lower_bound and std::upper_bound, std::equal_range still provides O(logn) number of comparisons on non-random-access ranges.
https://compiler-explorer.com/z/zzE93Tn5b

```c++

int main() {
    std::vector<int> data{1, 2, 3, 4, 4, 4, 6, 7, 8, 9, 10};

    {
    auto [low, upp] = std::equal_range(data.begin(), data.end(), 4);
    // [low, upp) == {4, 4, 4}
    // *low == 4, *upp == 6

    std::println("[low, upp] == {}, *low == {}, *upp == {}", std::ranges::subrange(low, upp), *low, *upp);
    }

    {
    auto [low, upp] = std::equal_range(data.begin(), data.end(), 5);
    // low == upp (empty range)
    // *low == 6, *upp = 6
    
    std::println("[low, upp] == {}, *low == {}, *upp == {}", std::ranges::subrange(low, upp), *low, *upp);
    }

    // Keep in mind that when using the range version with a projection,
    // it is the projected range that must be sorted
    std::vector<std::string> labels{"x", "aa", "xyz", "abc", "defg"};

    {
    // find the range of strings of length 3
    auto [low, upp] = std::ranges::equal_range(labels, 3, 
        std::less<>{},                       // comparator
        [](auto& s) { return s.length(); }); // project the strings to their length
    // [low, upp) == {"xyz", "abc"}
    // *low == "xyz", *upp == "defg"

    std::println("[low, upp] == {}, *low == {}, *upp == {}", std::ranges::subrange(low, upp), *low, *upp);
    }   
}
```
The conditional operator, also known as the ternary operator, is essentially an if statement in the form of an expression.

The operator is sequenced. The left side, with all its side effects, is evaluated first, followed by one of the right-side operands.

Before C++14, the ternary operator was the only way to inject logic into a constant-evaluated expression.
https://compiler-explorer.com/z/5shYvhsTa

```c++

struct A {};
struct B : A {};

struct MustBeInit {
    MustBeInit(int v) : v(v) {}
    int v;
};

int main() {
    // Simple logic:
    int x = 1;
    int y = 2;
    int abs_diff = x < y ? y - x : x - y;
    // abs_diff = 1

    std::println("abs_diff == {}", abs_diff);

    // Useful for initializing variables 
    // that do not support default initialization:
    MustBeInit m = true ? 1 : 2;
    // m.v == 1

    std::println("m.v == {}", m.v);

    // Before C++14, conditional expression was 
    // the only way to inject logic into constant-evaluted code
    constexpr bool has_many = true;
    std::array<int, has_many ? 1024 : 8> arr;

    // When mixing types, standard conversions are permitted, ex:
    auto r1 = true ? 2.0 : 1; // int->double
    // decltype(r1) == double
    auto r2 = true ? A() : B(); // class to base
    // decltype(r2) == A

    static_assert(std::is_same_v<decltype(r1), double>);
    static_assert(std::is_same_v<decltype(r2), A>);

    // Throw expressions are permitted, as they break flow:
    std::string* ptr = nullptr;
    try {
        auto len = ptr ? ptr->length() : 
            throw std::runtime_error("Null pointer dereference.");
        // decltype(len) == size_t
        
        static_assert(std::is_same_v<decltype(len), size_t>);
    } catch (...) {}

    // If both sides are throw expressions, the result is void
    auto r3 = []{ return true ? throw 1 : throw 2; };
    // decltype(r3()) == void

    static_assert(std::is_same_v<decltype(r3()), void>);

    // When mixing cv-qualifiers, the arguments need to overlap,
    // but the result is then the most-qualified type.
    int a = 1;
    const int& b = 2;
    int& c = a;

    auto &r4 = true ? b : c;
    // decltype(r4) == const int&

    static_assert(std::is_same_v<decltype(r4), const int&>);
}
```
The C++23 introduced a major change for the subscript operator, allowing it to take multiple arguments.

When multiple arguments are provided, they are separated by a comma. The standard comma operator in subscripts was deprecated in C++20.

This finally gives C++ a way to interface with multi-dimensional data structures naturally.
https://godbolt.org/z/EMvs3G4Mb

```c++

struct Maze {
    // operator[] can now take multiple arguments
    char& operator[](size_t row, size_t col) {
        if (row*width+col >= data.length())
            throw std::out_of_range("out of bounds access");
        return data[row*width+col];
    }
    const char& operator[](size_t row, size_t col) const {
        if (row*width+col >= data.length())
            throw std::out_of_range("out of bounds access");
        return data[row*width+col];
    }

    size_t height;
    size_t width;
    std::string data;
};


// Very simple (and not very ergonomic) 3D spare store
struct Sparse3D {
    // Mutating access, always creates the cell
    int64_t& operator[](int64_t x, int64_t y, int64_t z) {
        return store_[x][y][z];
    }
    // Const access path, only reads
    const int64_t& operator[](int64_t x, int64_t y, int64_t z) const {
        auto xi = store_.find(x);
        if (xi == store_.end()) return empty;
        auto yi = xi->second.find(y);
        if (yi == xi->second.end()) return empty;
        auto zi = yi->second.find(z);
        if (zi == yi->second.end()) return empty;
        return zi->second;
    }
private:
    std::unordered_map<int64_t, 
        std::unordered_map<int64_t, 
            std::unordered_map<int64_t, 
                int64_t>>> store_;
    constexpr static int64_t empty = 0;
};

// the operator[] now behaves exactly as operator()
// leading to some "interesting use-cases"
struct WeirdCallable {
    void operator()(const std::string& what, const std::string& where) const {
        std::println("I will {} you at the {}.", what, where);
    }
    void operator[](const std::string& what, const std::string& where) const {
        std::println("You will {} me at the {}.", what, where);
    }
};

int main() {
    Maze maze{4,4,"# ### ###   ####"};

    for (auto ridx : std::views::iota(0uz, 4uz)) {
        for (auto cidx : std::views::iota(0uz, 4uz)) {
            std::print("{}", maze[ridx,cidx]);
        }
        std::println("");
    }

    std::println("");

    Sparse3D data;
    // creates the cell and stores 20 in it
    data[0,0,0] = 20; 
    // reads an non-existent cell without creating it
    auto c1 = std::as_const(data)[0,1,2];
    // c1 == 0

    std::println("c1 == {}", c1);

    // reads the previously stored cell
    auto c2 = std::as_const(data)[0,0,0];
    // c2 == 20
    
    std::println("c2 == {}", c2);

    std::println("");

    // Perhaps something to forbid in your local style-guide
    WeirdCallable me;
    me("greet","market");
    // prints: "I will greet you at the market."
    me["hug","crossroad"];
    // prints: "You will hug me at the crossroad."
}
```
The std::in_range is a simple C++20 utility that checks whether the given runtime value is within the range of the given integer type.

This can be used to check whether a runtime conversion would be value-changing or, in the case of signed integers, would involve undefined behaviour.
https://compiler-explorer.com/z/W49cPWz7q

```c++

// Example of guarding a function with a limited domain.
namespace {
    // Actual implementation that needs to be guarded.
    void some_func_impl(uint16_t) {}
}

// Interface function accepts any integral
// guaranteeing no value-changing conversions.
void some_func(std::integral auto v) {
    if (!std::in_range<uint16_t>(v))
        throw std::out_of_range("value out of range");

    // Safe implicit conversion
    some_func_impl(v);
}

int main() {
    bool v1 = std::in_range<int>(-1);
    // v1 == true

    std::println("v1 == {}", v1);

    bool v2 = std::in_range<unsigned>(-1);
    // v2 == false

    std::println("v2 == {}", v2);

    try {
        // int, not in range of uint16_t
        some_func(-1); // throws
    } catch (...) {
        std::println("some_func(-1) throws");
    }

    // size_t literal, in range of uint16_t
    some_func(2uz); // calls some_func_impl

    try {
        // int, not in range of uint16_t
        some_func(UINT16_MAX+1); // throws
    } catch (...) {
        std::println("some_func(UINT16_MAX+1) throws");
    }
}
```
The C++20 standard introduced a new version of the standard thread handle, std::jthread (joining thread).

When an instance of a std::thread is destroyed while holding a thread, it will automatically call std::terminate. We, therefore, have to manually call join() or detach().

The std::jthread will instead automatically join the held thread in its destructor.
https://compiler-explorer.com/z/c11oW9obr

```c++

int main() {
    {
    auto t = std::thread([]{
        using namespace std::literals;
        std::this_thread::sleep_for(200ms);
    });
    // Required
    t.join();
    } // end of scope, t is destroyed

    {
    auto t = std::jthread([]{
        using namespace std::literals;
        std::this_thread::sleep_for(200ms);
    });
    } // end of scope, t is joined and then destroyed

    // Start a thread and immediately join.
    std::jthread([]{});
    // Start a thread and immediately call std::terminate.
    std::thread([]{});
}
```
C++20 introduced a standard tool for issuing stop requests: std::stop_source, std::stop_token, and std::stop_callback.

A std::stop_source can be used to request a stop, which can then be observed thread-safe through the associated std::stop_token. Any callbacks registered through a std::stop_callback will also be run when a stop is requested.
https://compiler-explorer.com/z/3s5b85MzY

```c++

int main() {
    using namespace std::literals;

    // A std::jthread will automatically pass a stop token as the first argument
    auto t1 = std::jthread([](std::stop_token token){
        // Run, until stop is requested:
        while (!token.stop_requested()) {
            std::println("T1\tThread {} running", std::this_thread::get_id());
            std::this_thread::sleep_for(100ms);
        }
        std::println("T1\tThread {} observed stop request", std::this_thread::get_id());
    });
    std::this_thread::sleep_for(300ms);
    std::println("Main\tThread {} requesting T1 stop", std::this_thread::get_id());
    t1.request_stop(); // request stop

    std::this_thread::sleep_for(1s);
    std::println("");

    auto t2 = std::jthread([](std::stop_token token) {
        // Alternatively, a callback can be invoked when a stop is requested.
        // The callback is executed either on the thread requesting the stop,
        // or if the stop is already requested when the callback is registered
        // the callback runs on the thread registering the callback.
        std::atomic<bool> flag = false;
        std::stop_callback callback(token, [&flag]{
            std::println("T2\tThread {} observed stop request", std::this_thread::get_id());
            flag = true;
        });

        while (not flag) {
            std::println("T2\tThread {} running", std::this_thread::get_id());
            std::this_thread::sleep_for(100ms);
        }
    });

    std::this_thread::sleep_for(300ms);
    std::println("Main\tThread {} requesting T2 stop", std::this_thread::get_id());
    t2.request_stop(); // runs any already associated callbacks on this thread


    std::this_thread::sleep_for(1s);
    std::println("");

    struct Resource {
        std::mutex mux;
        std::condition_variable_any cv;
        bool ready = false;
    };
    Resource resource;
    auto t3 = std::jthread([&resource](std::stop_token token) {
        std::println("T3\tThread {} running", std::this_thread::get_id());
        // Wait until resource is ready, or stop was requested:
        std::unique_lock lock(resource.mux);
        resource.cv.wait(lock, token, [&resource] { return resource.ready; });

        if (resource.ready) { // resource is ready
            std::println("T3\tThread {} done, resource ready", std::this_thread::get_id());
        } else { // stop was requested
            std::println("T3\tThread {} done, stop requested", std::this_thread::get_id());
        }
    });

    std::this_thread::sleep_for(100ms);
    if (true) { // change to see the other option
        std::println("Main\tThread {} requesting stop for condition variable", std::this_thread::get_id());
        t3.request_stop(); // request stop
    } else {
        std::println("Main\tThread {} marking resource as ready", std::this_thread::get_id());
        std::unique_lock lock(resource.mux);
        resource.ready = true;
    }
}
```
The std::partition_copy algorithm is a variant of std::partition that, instead of operating in place, writes the two partitions through the two provided output iterators.

The algorithm provides both a C++17 parallel version and a C++20 range version.
https://compiler-explorer.com/z/WxTqcr9n6

```c++

int main() {
    std::vector<std::string> vowels, consonants;

    std::ranges::partition_copy(
        std::views::istream<std::string>(std::cin),
        std::back_inserter(vowels),     // iterator for condition == true
        std::back_inserter(consonants), // iterator for condition == false
        [](const std::string& s){
            // Check if first character is a vowel:
            char c = std::tolower(s.front());
            return (c == 'a' || c == 'e' || c == 'i' || 
                    c == 'o' || c == 'u');
        });
    // For input "Hello, World! This is going to be a blast.":
    // vowels == {"is", "a"}
    // consonants == {"Hello,", "World!", "This", "going", 
    //                "to", "be", "blast."}

    std::println("Vowels: {}", vowels);
    std::println("Consonants: {}", consonants);
}
```
𝒔𝒕𝒅::𝒎𝒖𝒕𝒆𝒙 is a mutual exclusion lock that permits only one owning thread to hold the lock, protecting a shared resource from simultaneous access by multiple threads.

Because manually unlocking the 𝒔𝒕𝒅::𝒎𝒖𝒕𝒆𝒙 is potentially error-prone, using an RAII-based wrapper such as 𝒔𝒕𝒅::𝒖𝒏𝒊𝒒𝒖𝒆_𝒍𝒐𝒄𝒌 is highly recommended.
https://compiler-explorer.com/z/ePdojjEPj

```c++

struct Shared {
    int value;
    std::mutex mux;
};

int main() {
    Shared shared{0,{}};

    {
    auto t1 = std::jthread([&shared]{
        for (int i = 0; i < 10; i++) {
            // obtain lock
            std::unique_lock lock(shared.mux);
            // safely modify shared state
            shared.value += 10;
        } // std::unique_lock is destroyed and mutex is unlocked
    });

    auto t2 = std::jthread([&shared]{
        for (int i = 0; i < 10; i++) {
            // obtain lock
            std::unique_lock lock(shared.mux);
            // safely modify shared state
            shared.value += 1;
        } // std::unique_lock is destroyed and mutex is unlocked
    });
    } // t1 && t2 a destroyed and joined

    // shared.value == 110
    std::println("shared.value == {}", shared.value);
}
```
The std::recursive_mutex is a mutex variant that can be locked multiple times by the same thread. Other threads can only acquire a lock on the mutex after all held locks have been released.

This can significantly simplify code where several functions need to acquire the lock and call each other.
https://compiler-explorer.com/z/36K7qTYPE

```c++

struct NonRecursive {
    void push_back(int value) {
        std::lock_guard lock(mux_);
        // we already hold mux_, so we can't directly call reserve
        if (size_ == capacity_)
            reserve_impl(capacity_ == 0 ? 64 : capacity_ * 2);
        data_[size_++] = value;
    }
    void reserve(size_t cnt) {
        std::lock_guard lock(mux_);
        reserve_impl(cnt);
    }
private:
    // reserve_impl expects mux_ to be held by the caller
    void reserve_impl(size_t cnt) {
        auto new_data = std::make_unique_for_overwrite<int[]>(cnt);
        std::ranges::move(std::span(data_.get(), std::min(size_, cnt)),
                          new_data.get());
        data_ = std::move(new_data);
        capacity_ = cnt;
        size_ = std::min(size_, capacity_);
    }

    std::mutex mux_;
    std::unique_ptr<int[]> data_;
    size_t size_ = 0;
    size_t capacity_ = 0;
};

struct Recursive {
    void push_back(int value) {
        std::lock_guard lock(mux_);
        // holding a recursive mutex multiple times is fine
        if (size_ == capacity_)
            reserve(capacity_ == 0 ? 64 : capacity_ * 2);
        data_[size_++] = value;
    }
    void reserve(size_t cnt) {
        std::lock_guard lock(mux_);
        auto new_data = std::make_unique_for_overwrite<int[]>(cnt);
        std::ranges::move(std::span(data_.get(), std::min(size_, cnt)),
                          new_data.get());
        data_ = std::move(new_data);
        capacity_ = cnt;
        size_ = std::min(size_, capacity_);
    }
private:
    std::recursive_mutex mux_;
    std::unique_ptr<int[]> data_;
    size_t size_ = 0;
    size_t capacity_ = 0;
};

int main() {}
```
The std::shared_mutex is a std::mutex variant that supports two types of locks: an exclusive lock that can be held by only one thread and a shared lock that can be held by any number of threads (as long as the exclusive lock is not held).

The typical use case for exclusive/shared lock semantics is a shared resource with read/write access.
https://compiler-explorer.com/z/Mjq5sd65s

```c++

template< class T, class U >
concept type_as = std::same_as<std::remove_cvref_t<T>, std::remove_cvref_t<U>>;

struct Data {};
struct RecentSnapshots {

    void push(type_as<Data> auto&& data) {
        // trick to avoid duplicate const T& and T&& implementations

        // We are about to modify the data, grab a unique_lock
        std::unique_lock lock(mux_);

        buffer_[offset_ % 64] = std::forward<decltype(data)>(data);
        ++offset_;
    } // exclusive lock automatically released

    std::optional<Data> get(size_t index) const {
        // We only read, but need to prevent 
        // concurrent writes, grab a shared_lock
        std::shared_lock lock(mux_);

        // The requested index doesn't exist yet
        if (index >= offset_)
            return std::nullopt;
        // The requested index no longer exists
        if (offset_ >= 64 && offset_-64 > index)
            return std::nullopt;

        return buffer_[index % 64];
    } // shared lock automatically released

    std::optional<size_t> oldest_index() const {
        // We only read, but need to prevent 
        // concurrent writes, grab a shared_lock
        std::shared_lock lock(mux_);

        if (offset_ == 0)
            return std::nullopt;

        return std::sub_sat<size_t>(offset_,64); // C++26 subtraction without underflow
    } // shared lock automatically released

private:
    // We need mutable, since we mutate the state
    // of this mutex (by grabbing a lock) in const methods.
    mutable std::shared_mutex mux_;
    std::array<Data,64> buffer_;
    size_t offset_ = 0;
};

// Note: calling oldest_index() followed by get(offset)
// does NOT provide any transactionality, as a write can interject
// itself between the two calls.


int main() {
    RecentSnapshots snapshots;
    assert(not snapshots.oldest_index());

    snapshots.push(Data{});
    snapshots.push(Data{});

    assert(snapshots.oldest_index() == 0);

    assert(snapshots.get(0));
    assert(snapshots.get(1));
    assert(not snapshots.get(2));
}
```
The std::adjacent_find is a linear find algorithm that returns an iterator to the first pair of adjacent elements that satisfy the provided binary predicate.

The iterator will point to the first of the two elements.

The algorithm has both the C++17 parallel execution variant and the C++20 ranges version.
https://compiler-explorer.com/z/xnd67jTo5

```c++

int main() {
    std::vector<int> data{1,2,3,4,4,5,6,7,7,8,9};

    // find the first pair of adjacent elements that are equal
    auto it = std::adjacent_find(data.begin(), data.end(), std::equal_to<>{});
    // *it == 4

    std::println("*it == {}", *it);

    std::string delimited = "this is a string\"(with an embeded message)\" that continues";
    // Find the sequence "(
    auto start = std::adjacent_find(delimited.begin(), delimited.end(), [](char l, char r) {
        return l == '"' && r == '(';
    });
    // Find the sequence )" that follows "(
    auto end = std::adjacent_find(start, delimited.end(), [](char l, char r) {
        return l == ')' && r == '"';
    });
    std::string embeded(start+2, end); // skip over "(
    // embeded == "with an embeded message"
    delimited.erase(start, end+2); // also erase )"
    // delimited == "this is a string that continues"

    std::println("embeded == \"{}\"", embeded);
    std::println("delimited == \"{}\"", delimited);

    std::vector<std::string> labels{"a", "bc", "de", "fgh"};

    // Find a pair of adjacent strings with the same length
    auto len = std::ranges::adjacent_find(labels, std::equal_to<>{}, [](const auto& l) {
            return l.length();
        });
    // *len == "bc"

    std::println("*len == \"{}\"", *len);
}
```
When a thread attempts to acquire a lock on a mutex, it will block until it is acquired, potentially indefinitely.

However, when working with operations that have a timeout/deadline, there is no point in waiting for a lock past the deadline.

The standard offers timed_mutex, recursive_timed_mutex and shared_timed_mutex, that support timeouts for the try_lock method.
https://compiler-explorer.com/z/Yos3vs13d

```c++

int main() {
    using namespace std::chrono_literals;

    std::timed_mutex mux;
    // This thread will hold the lock for 2 seconds
    auto t = std::jthread([&mux]{
        std::unique_lock lock(mux);
        std::this_thread::sleep_for(2s);
    });

    auto runner = [&mux]{
        // Try to obtain the lock, give up after 200ms
        std::unique_lock lock(mux, 200ms);
        // internally calls:
        // for a time point: mux.try_lock_until()
        // for a duration:   mux.try_lock_for()
        if (not lock.owns_lock()) {
            std::println("Unable to obtain lock");
            return;
        }
        std::println("Lock was obtained");
    };

    std::this_thread::sleep_for(100ms);
    // Generally, the first two attempts will time out 
    // the other two attempts should succeed.
    for (size_t i = 0; i < 4; ++i) {
        auto r = std::jthread(runner);
        std::this_thread::sleep_for(1s);
    }
}
```
std::condition_variable and std::condition_variable_any allow multiple threads to wait on a shared state safely.

The std::condition_variable only supports std::unique_lock, std::condition_variable_any supports std::stop_token and will work with any lock that provides lock() and unlock() methods.
https://compiler-explorer.com/z/67339f1Wf

```c++
using namespace std::chrono_literals;

struct Resource {
    // Shared state
    bool full = false;    
    std::mutex mux;

    std::condition_variable_any cond;
    void produce(std::stop_token token) {
        {
        std::unique_lock lock(mux);
        // wait until the condition is true
        // 1. the lock is released
        // 2. when the thread is woken up, the lock is reacquired
        // 3. if the stop was requested, wait() call finishes, returning false
        // 4. if the stop wasn't requested, the condition is checked
        // 5. if the condition is still not true, the lock is rereleased, and we go to step 2.
        // 6. if the condition is true, the wait() call finishes, returning true
        if (cond.wait(lock, token, [this]{ return !full; })) {
            std::println("Producer running.");
        } else {
            std::println("Producer stopping.");
            return;
        }
        // When wait exits, the lock is held
        std::println("Filling the resource and notifying the consumer.");
        full = true; // safe
        std::this_thread::sleep_for(200ms);
        }
        // wake up one thread waiting on this condition variable
        // note that we already released our lock, otherwise
        // the notified thread would wake up and fail to acquire
        // the lock and suspend itself again
        cond.notify_one();
    }
    void consume(std::stop_token token) {
        {
        std::unique_lock lock(mux);
        // same as above, but with opposite semantics
        if (cond.wait(lock, token, [this]{ return full; })) {
            std::println("Consumer running.");
        } else {
            std::println("Consumer stopping.");
            return;
        }
        std::println("Consuming the resource and notifying the producer.");
        full = false;
        std::this_thread::sleep_for(200ms);
        }
        cond.notify_one();
    }
};

int main() {
    Resource resource;
    // Start a consumer and a producer
    auto t1 = std::jthread([&resource](std::stop_token token){
        while (!token.stop_requested())
            resource.produce(token);
    });
    auto t2 = std::jthread([&resource](std::stop_token token){
        while (!token.stop_requested())
            resource.consume(token);
    });
    std::this_thread::sleep_for(2s);
    // Request both to stop
    t1.request_stop();
    t2.request_stop();
}
```
The C++20 std::shift_left and std::shift_right algorithms move elements in the provided range by the specified offset.

Both algorithms internally move, meaning we can simulate these algorithms manually by calling the std::move algorithm with the appropriate source and offset destination.

Both algorithms have parallel (C++20) and range (C++23) versions.
https://compiler-explorer.com/z/4rer4o945

```c++

struct EmptyOnMove {
    char value;
    EmptyOnMove(char value) : value(value) {}
    EmptyOnMove(EmptyOnMove&& src) : value(std::exchange(src.value,'-')) {}
    EmptyOnMove& operator=(EmptyOnMove&& src) {
        value = std::exchange(src.value, '-');
        return *this;
    }
    EmptyOnMove(const EmptyOnMove&) = default;
    EmptyOnMove& operator=(const EmptyOnMove&) = default;
};

int main() {
    // For trivial types, moves behaves as a copy
    std::vector<int> data{1,2,3,4,5,6,7,8,9};
    std::shift_left(data.begin(), data.end(), 3);
    // data == {4, 5, 6, 7, 8, 9, 7, 8, 9}

    std::println("data == {}", data);

    // Same as:
    data = {1,2,3,4,5,6,7,8,9};
    std::move(data.begin()+3, data.end(), data.begin());
    // data == {4, 5, 6, 7, 8, 9, 7, 8, 9}

    std::println("data == {}", data);

    data = {1,2,3,4,5,6,7,8,9};
    std::shift_right(data.begin(), data.end(), 3);
    // data == {1, 2, 3, 1, 2, 3, 4, 5, 6}

    std::println("data == {}", data);

    // Same as:
    data = {1,2,3,4,5,6,7,8,9};
    std::move_backward(data.begin(), data.end()-3, data.end());
    // data == {1, 2, 3, 1, 2, 3, 4, 5, 6}

    std::println("data == {}", data);

    // Demonstration of the move with a type 
    // that sets its value to '-' when moved from.
    std::vector<EmptyOnMove> nontrivial{{'a'},{'b'},{'c'},{'d'},{'e'},{'f'},{'g'}};
    std::shift_right(nontrivial.begin(), nontrivial.end(), 4);
    // nontrivial == {---dabc}
    // abc were moved, hence source elements have value '-'
    // d wasn't moved

    std::println("nontrivial == {}", nontrivial | std::views::transform(&EmptyOnMove::value));
}
```
std::barrier is a C++20 synchronization primitive that enables the creation of synchronized execution phases across multiple threads.

A std::barrier is initialized with a count (the number of threads). When a thread arrives at a barrier, it can block until all other threads arrive or drop, decreasing the counter.
https://compiler-explorer.com/z/ezP3Ydao8

```c++

int main() {
    // Barrier with a completion function that prints
    // the phase information:
    std::barrier phase(4,[id = 1] mutable {
        std::println("Phase {} complete.", id);
        id++;
    });

    std::vector<std::jthread> runners;
    // Start 4 threads:
    std::generate_n(std::back_inserter(runners), 4, [&phase]{
        return std::jthread([&phase]{
            std::println("Running phase 1 for thread {}", std::this_thread::get_id());

            std::this_thread::yield();
            // block until all threads arrive
            phase.arrive_and_wait();

            std::println("Running phase 2 for thread {}", std::this_thread::get_id());

            std::this_thread::yield();
            // block until all threads arrive
            phase.arrive_and_wait();
        });
    });
    runners.clear(); // join the threads

    // This is guaranteed to print:
    // Running phase 1 for thread xxxxx (for each of the four threads)
    // Phase 1 complete.
    // Running phase 2 for thread xxxxx (for each of the four threads)
    // Phase 2 complete.

    std::println("");

    // Barrier without a custom completion function:
    std::barrier other(5);
    std::vector<std::jthread> runners2;
    // Start 5 threads:
    std::generate_n(std::back_inserter(runners2), 5, [&other]{
        return std::jthread([&other]{
            // Use thread id hash as the seed to get different behaviour for each thread
            std::mt19937 gen(std::hash<std::thread::id>{}(std::this_thread::get_id()));
            std::bernoulli_distribution done(0.3); // 30% chance to produce true, 70% false

            int id = 1;
            while (true) {
                std::println("Running phase {} for thread {}", id, std::this_thread::get_id());

                std::this_thread::yield();
                if (done(gen)) { // 30% chance for the thread to stop
                    // decrease the barrier initial counter
                    // so that it waits for n-1 threads
                    other.arrive_and_drop();
                    return;
                }
            
                // Otherwise block until all (still running) threads arrive.
                other.arrive_and_wait();
                ++id;
            }
        });
    });
    // This will print consecutive phases, with the number of 
    // threads in each phase randomly decreasing.
}
```
The std::unique algorithm is typically used on a sorted range to produce the list of unique values.

The algorithm will operate on any forward range and will remove consecutive duplicate values.

The copy variant std::unique_copy will emit the unique values through the provided output iterator instead.

Both algorithms provide a C++17 parallel, and C++20 ranges variant.
https://compiler-explorer.com/z/hxa9MbxnY

```c++

int main() {
    std::vector<int> data{1, 2, 2, 3, 2, 3, 3, 1, 1, 1};

    // Remove duplicates, by shifting elements forward
    // returns an iterator to the new end
    auto it = std::unique(data.begin(), data.end());
    auto uniq = std::ranges::subrange(data.begin(), it);
    // data == {1, 2, 3, 2, 3, 1, ?, ?, ?, ?}
    // uniq == {1, 2, 3, 2, 3, 1}

    std::println("data == {}", data);
    std::println("uniq == {}", uniq);

    // To actually remove the elements we need to call erase
    data.erase(it, data.end());
    // data == {1, 2, 3, 2, 3, 1}

    std::println("data == {}", data);

    data = {1, -2, 2, 3, -2, 3, -3, 1, -1, 1};
    std::vector<int> out;

    // Copy variant outputs the unique elements through
    // the provided iterator
    std::unique_copy(data.begin(), data.end(), std::back_inserter(out),
        // Both algorithms support custom comparison function
        [](int l, int r) {
            return std::abs(l) == std::abs(r);
        });
    // out == {1, -2, 3, -2, 3, 1}

    std::println("out  == {}", out);

    out.clear();
    // Keep in mind that the projection in the C++20 range version 
    // only applies to the comparator, and doesn't affect 
    // the values written/copied
    std::ranges::unique_copy(data, std::back_inserter(out),
        std::equal_to<>{},
        [](int l) { return std::abs(l); });
    // out == {1, -2, 3, -2, 3, 1}

    std::println("out  == {}", out);
}
```
The std::promise and std::future are high-level synchronization tools that implement one-shot producer-consumer semantics.

A producer can fill the state of a std::promise with either a value or an exception. A consumer holding a std::future associated with this std::promise can call get() to block until the shared state is available, at which point get() returns the state.
https://compiler-explorer.com/z/jcsoY17s4

```c++

int main() {
    using namespace std::literals;

    std::promise<std::string> promise;
    // Future is obtained from the promise.
    std::future<std::string> future = promise.get_future();
    auto t1 = std::jthread([promise = std::move(promise)] mutable {
        std::this_thread::sleep_for(100ms);
        // Set the value, this will unblock the consumer (future).
        promise.set_value("Hello World!"s);
        // If it is preferable to block until this thread finishes:
        // promise.set_value_at_thread_exit("Hello World!"s);
    });

    // Will block until value awailable, 
    // then returns the stored value:
    std::println("future.get() == {}", future.get());

    // Promise/Future can also propagate exceptions:
    std::promise<int> other;
    std::future<int> will_fail = other.get_future();
    auto t2 = std::jthread([promise = std::move(other)] mutable {
        try {
            throw std::runtime_error("Some error happened.");
            promise.set_value(10); // unreachable
        } catch (...) {
            promise.set_exception(std::current_exception());
            // same as before we can also:
            // promise.set_exception_at_thread_exit(std::current_exception());
        }
    });

    try {
        // Block until value awailable, in this case,
        // the exception will be propagated instead.
        int v = will_fail.get();
        std::println("Unreachable, will not print. v == {}", v);
    } catch (const std::exception& e) {
        std::println("Caught a propagated exception: e.what() == {}", e.what());
    }

    std::promise<void> slow;
    std::future<void> will_timeout = slow.get_future();
    auto t3 = std::jthread([promise = std::move(slow)] mutable {
        // Sleep, causing a timeout for the consumer
        std::this_thread::sleep_for(2s);
        promise.set_value();
    });

    // Wait for 200ms (which will timeout)
    if (will_timeout.wait_for(200ms) == std::future_status::timeout) {
        std::println("Future did not receive state within 200ms, bailing out.");
    } else {
        // If we didn't timeout, calling get() will not block
        // (in general could also be a deferred function)
        will_timeout.get();
        std::println("Future fulfilled.");
    }
}
```
The std::inplace_merge algorithm merges two sorted subranges into one sorted range.

With this algorithm, we can quickly build a merge-sort implementation (as the in-place merge is the core of this sort).

The merge is stable (maintains the order of equal elements), making the sort implementation also stable.

The algorithm has both a parallel C++17 and range C++20 version.
https://compiler-explorer.com/z/a19jT1EsG

```c++

void merge_sort(std::ranges::random_access_range auto& rng) {
    if (rng.size() <= 1) return;

    // divide the range into two parts
    auto mid = rng.begin() + rng.size()/2;
    auto left = std::ranges::subrange(rng.begin(), mid);
    auto right = std::ranges::subrange(mid, rng.end());

    // recursive sort left and right
    merge_sort(left);
    merge_sort(right);
    // in-place merge left and right
    std::ranges::inplace_merge(rng, mid);
}


int main() {
    std::vector<int> data{3, 5, 1, 4, 2, 6};
    merge_sort(data);

    for (auto v : data)
        std::print("{} ", v);
    std::println("");
}
```
The C++17 std::scoped_lock is an RAII lock that provides a library-level solution for acquiring locks on multiple mutexes without risking a deadlock.

When locks are not always acquired in the same order, we can trivially introduce a deadlock when thread T1 holds lock A and tries to acquire lock B while thread T2 holds lock B and tries to acquire lock A.
https://compiler-explorer.com/z/9j7q1bq6n

```c++

struct Player {
    Player(std::string name, uint64_t seed) : name_(std::move(name)),
        re_(seed), dist_(1,6), mux_{}, score_{0} {}

    void play_with(Player& other) {
        if (&other == this) return;
        // to play a game we need to obtain both our lock and the lock
        // of the oponent which creates potential for deadlock
        std::scoped_lock lock(mux_, other.mux_);
        // roll the dice until one player wins, then increase the score
        int our = 0, them = 0;
        do {
            our = roll();
            them = other.roll();
        } while (our == them);
        if (our > them)
            score_++;
        else
            other.score_++;
    } // lock released
    const std::string& name() const { return name_; }
    int score() const { return score_; }
private:
    // Roll a D6
    int roll() { return dist_(re_); }
    std::string name_;
    std::default_random_engine re_;
    std::uniform_int_distribution<int> dist_;
    std::mutex mux_;
    int score_;
};

int main() {
    std::random_device r;
    std::vector<std::unique_ptr<Player>> players;
    auto names = {"Player1", "Player2", "Player3", "Player4",
                  "Player5", "Player6", "Player7", "Player8",
                  "Player9"};
  	// generate players from the names
    std::ranges::transform(names, std::back_inserter(players),
    	[&](const char* name) {
          return std::make_unique<Player>(name, r()); 
        });

    // Run the tournament:
    // each player plays all other players in parallel
    std::vector<std::jthread> rounds;
    for (auto &v : players) {
        rounds.push_back(std::jthread([&players,&v]{
            for (auto &oponent : players) {
                v->play_with(*oponent);
            }
        }));
    }
    rounds.clear(); // a.k.a. join all threads

    // Sort and print
    std::ranges::sort(players, std::greater<>{}, 
    	[](const std::unique_ptr<Player>& p) {
          return p->score();
        });
    for (const auto &v : players) {
        std::cout << v->name() << " " << v->score() << "\n";
    }
}
```
The std::to_underlying is a C++23 single-purpose cast that converts an enumerator to the underlying integral type of the enumeration.

This single-step approach replaces the previous approach using static_cast in combination with the C++11 std::underlying_type utility, which is required because the underlying type of unscoped enumerations is implementation-defined.
https://compiler-explorer.com/z/enz8j67of

```c++

enum Color {
    RED,
    GREEN,
    BLUE
};

enum Info {
    LIMIT = UINT64_MAX
};

enum class MyColor {
    RED,
    GREEN,
    BLUE
};

int main() {
    auto a = std::to_underlying(RED);
    // a == 0, sizeof(a) == sizeof(int) (concrete type is impl.defined)

    std::println("a == {}, sizeof(a) == {}, sizeof(int) == {}", a, sizeof(a), sizeof(int));

    auto b = std::to_underlying(LIMIT);
    // b == UINT64_MAX, decltype(b) an unsigned type of size uint64_t

    std::println("b == {}, sizeof(b) == {}, sizeof(uint64_t) == {}", b, sizeof(b), sizeof(uint64_t));

    // Before C++23, we would have to rely on static_cast
    auto c = static_cast<std::underlying_type<Info>::type>(LIMIT); // C++11
    auto d = static_cast<std::underlying_type_t<Info>>(LIMIT); // C++14
    // b == c == d

    std::println("b == {}, c == {}, d == {}", b, c, d);

    // Scoped enumerations have a well defined underlying type
    auto e = std::to_underlying(MyColor::GREEN);
    // e == 1, decltype(e) == int

    std::println("e == {}", e);
    static_assert(std::is_same_v<decltype(e), int>);
}
```
The std::call_once is a low-level synchronization tool that guarantees a single successful call (i.e. a call that doesn&#39;t throw) to a callable.

Each call attempt synchronizes with the exit of the previous attempt, and the exit of the successful call synchronizes with all concurrent calls, making any state changes visible to all threads without additional synchronization.
https://compiler-explorer.com/z/ob9xj3761

```c++

namespace {
std::vector<int> data;
std::once_flag flag;

// an operation that will fail twice and then succeed
void read_data() {
    static int fail_count = 2;
    if (fail_count != 0) {
        --fail_count;
        throw std::runtime_error{"Requested to fail"};
    }
    data = {1,2,3,4,5};
}
}

int sum() {
    // call read_data() once across all calls using flag
    std::call_once(flag, read_data);
    // additionally, we can pass arguments to the callable:
    // std::call_once(flag, read_data, arg1, arg2, ...);
    return std::accumulate(data.cbegin(), data.cend(), 0, std::plus<>{});
}

int product() {
    std::call_once(flag, read_data);
    return std::accumulate(data.cbegin(), data.cend(), 1, std::multiplies<>{});
}

int main() {
    // Run multiple instances of sum() and product() in parallel
    std::vector<std::jthread> runners;
    std::generate_n(std::back_inserter(runners), 4, []{
        return std::jthread([]{
            try {
                std::println("sum == {}", sum());
            } catch(const std::exception& e) {
                std::println("failed to calculate sum: {}", e.what());
            }
        });
    });
    std::generate_n(std::back_inserter(runners), 4, []{
        return std::jthread([]{
            try {
                std::println("product == {}", product());
            } catch(const std::exception& e) {
                std::println("failed to calculate product: {}", e.what());
            }
        });
    });
}
```
std::tuple is a heterogeneous collection of elements, a generalization of std::pair.

Unlike std::pair, a std::tuple can contain any number (including none) of elements.
https://compiler-explorer.com/z/Kxohdc1fM

```c++

void function(int a, int b) {
    std::println("function called with a == {}, b == {}", a, b);
}

int main() {
    // Empty std::tuple
    auto a = std::tuple{};
    // std::tuple_size_v<decltype(a)> == 0

    std::println("std::tuple_size_v<decltype(a)> == {}", std::tuple_size_v<decltype(a)>);

    auto b = std::tuple{5,42}; // CTAD, deduced to std::tuple<int,int>
    // pre-CTAD you could use std::make_tuple(5, 42) for the same result

    static_assert(std::is_same_v<decltype(b), std::tuple<int,int>>);

    // construct a std::vector using the tuple elements as arguments to the constructor
    auto v = std::make_from_tuple<std::vector<int>>(b);
    // v == {42, 42, 42, 42, 42}

    std::println("v == {}", v);

    // call a function, using the tuple elements as arguments
    std::apply(function, b);

    // querying/access functions
    size_t cnt = std::tuple_size_v<decltype(b)>;
    // cnt == 2
    using e_t = std::tuple_element_t<0, decltype(b)>;
    // e_t == int
    int e = std::get<0>(b);
    // e == 5

    std::println("cnt == {}, e == {}", cnt, e);
    static_assert(std::is_same_v<e_t, int>);

    // make_tuple supports reference_wrapper to express references
    int x = 7, y = 42;
    auto t1 = std::make_tuple(x,y);
    // decltype(t1) == std::tuple<int,int>

    static_assert(std::is_same_v<decltype(t1), std::tuple<int,int>>);

    auto t2 = std::make_tuple(std::ref(x),y);
    // decltype(t2) == std::tuple<int&,int>

    static_assert(std::is_same_v<decltype(t2), std::tuple<int&,int>>);

    auto t3 = std::make_tuple(std::ref(x),std::cref(y));
    // decltype(t3) == std::tuple<int&,const int&>

    static_assert(std::is_same_v<decltype(t3), std::tuple<int&,const int&>>);

    // forward_as_tuple creates a tuple of fowarding references
    auto t4 = std::forward_as_tuple(std::move(x), std::move(y));
    // decltype(t4) == std::tuple<int&&,int&&>

    static_assert(std::is_same_v<decltype(t4), std::tuple<int&&,int&&>>);

    auto t5 = std::forward_as_tuple(x, std::move(y));
    // decltype(t5) == std::tuple<int&,int&&>

    static_assert(std::is_same_v<decltype(t5), std::tuple<int&,int&&>>);

    // as with std::pair, std::tuple can be deconstructed using structured bindings
    auto c = std::tuple{"Hello World!", 42, &a};
    auto [msg, val, ptr] = c;
    // msg == "Hello World!", val == 42, ptr == &a

    std::println("msg == {}, val == {}, ptr == {}, &a == {}", msg, val, (void*)ptr, (void*)&a);
}
```
The consteval specifier was introduced in C++20 to denote functions strictly evaluated at compile time.

This contrasts the behaviour of the constexpr specifier, which allows a function to be evaluated at compile time or runtime. While this can be a benefit (we avoid code duplication), it is also a potential detriment (it can be tricky to force compile-time evaluation).
https://compiler-explorer.com/z/MnM6EaWdE

```c++

constexpr int sum(int a, int b) {
    return a + b;
}
consteval int product(int a, int b) {
    return a * b;
}

consteval int product_until(int limit) {
    int result = 1;
    for (int i = 2; i <= limit; ++i) {
        // result and i are not constant expressions
        // but we are in a consteval function
        // therefore we can call product
        result = product(result, i);
    }
    return result;
}

// Wrapper that forces compile-time evaluation
consteval decltype(auto) at_compile(auto&& arg) {
    return std::forward<decltype(arg)>(arg);
}

int main(int argc, char**) {
    // Both constexpr and consteval can be used in constant expressions
    static_assert(sum(1,2) == 3);
    static_assert(product(2,3) == 6);

    int a = sum(argc,argc);
    // a == 2 (for argc == 1)

    // Wouldn't compile, argc is not a constant expression
    // int b = product(argc,argc);

    static_assert(product_until(5) == 120);

    // sum is now forced to be evaluated at compile-time
    int c = at_compile(sum(1,2));
    // Wouldn't compile, not a constant expression
    // int d = at_compile(sum(argc,argc));
}
```
C++23 added the 𝒊𝒇 𝒄𝒐𝒏𝒔𝒕𝒆𝒗𝒂𝒍 statement, which can test whether the code is invoked from a constant-evaluated context.

This allows for functions that couldn&#39;t be constant-evaluated to provide alternate implementations.

Note that while similar, this is different than 𝒊𝒇 𝒄𝒐𝒏𝒔𝒕𝒆𝒙𝒑𝒓, for which the condition is always evaluated in a constant context.
https://compiler-explorer.com/z/j8PbdfzT3

```c++

constexpr unsigned add(unsigned a, unsigned b) {
    if consteval {
        // consteval branch that can only use constant expression compatible code.
        return a + b;
    } else {
        // not consteval, we can use fancy things like inline assembler, or intristics
        unsigned ret;
        asm("addl %%ebx, %%eax;":"=a"(ret) : "a"(a), "b"(b));
        return ret;
    }
}

constexpr int side(int arg) {
    // if consteval allows us to include side effects such as logging
    // in functions that also need to be constant evaluated
    if not consteval {
        std::println("Not a constant expression, we can safely log.");
    }
    return arg*arg;
}


int main() {
    std::mt19937 gen;
    unsigned v = gen();

    unsigned r1 = add(v,v); // Will use the asm branch

    std::println("v == {}, add(v,v) == {}", v, r1);

    constinit static unsigned r2 = add(20,30); // Will use the non-asm branch

    std::println("add(20,30) == {}", r2);

    static_assert(add(20,30) == 50); // Also non-asm branch

    unsigned r3 = side(3); // Will log

    std::println("side(3) == {}", r3);

    static_assert(side(3) == 9); // No logging

    return 0;
}
```
The std::iter_swap is a convenient shorthand for indirectly swapping values, i.e. swapping the values pointed to by two iterators.

While the original version std::iter_swap is only intended for iterators, the range C++20 version std::ranges::iter_swap can work with any indirectly movable type and supports customization through ADL.
https://compiler-explorer.com/z/58P96cPdM

```c++

template <std::bidirectional_iterator It, 
          std::sentinel_for<It> Sentinel>
void reverse(It begin, Sentinel end) {
    while (begin != end) {
        end = std::prev(end);
        if (end == begin) break;
        // using iter_swap to swap the values behind the iterators
        std::iter_swap(begin,end);
        begin = std::next(begin);
    }
}

struct Indirect {
    int* value_;

    // Customizing iter_swap using a hidden friend
    friend void iter_swap(Indirect& l, Indirect& r) {
        std::println("iter_swap() customization called");
        int tmp = *l.value_;
        *l.value_ = *r.value_;
        *r.value_ = tmp;
    }
};

int main() {
    std::vector<int> data{1,2,3,4,5};
    reverse(data.begin(), data.end());
    // data == {5, 4, 3, 2, 1}

    std::println("data == {}", data);

    reverse(data.begin()+1, data.end()-1);
    // data == {5, 2, 3, 4, 1}

    std::println("data == {}", data);

    // C++20 ranges
    auto a = std::make_unique<int>(7);
    auto b = std::make_unique<int>(42);
    // Range version of iter_swap works for any indirectly movable type
    std::ranges::iter_swap(a,b);
    // *a == 42, *b == 7

    std::println("*a == {}, *b == {}", *a, *b);

    int x = 10, y = 20;
    Indirect xi(&x), yi(&y);
    // Calls iter_swap customization for Indirect
    std::ranges::iter_swap(xi,yi);
    // x == 20, y == 10

    std::println("x == {}, y == {}", x, y);
}
```
The std::nth_element is a partitioning algorithm with linear complexity that reorders the elements of the given range so that the element under the pivot iterator is the element that would be there if the range were sorted.

The algorithm does have a significant constant overhead, making std::partial_sort potentially faster for extreme percentiles and short ranges.
https://compiler-explorer.com/z/zzoPoz4nh

```c++

int main() {
    std::vector<int> data{8, 6, 2, 4, 3, 5, 9, 1};
    
    std::nth_element(data.begin(), data.begin()+3, data.end());
    // *(data.begin()+3) == 4
    // because the sorted range would be {1, 2, 3, 4...}

    std::println("*(begin+3) == {}", *(data.begin()+3));

    // The range version has the pivot a the second argument 
    std::ranges::nth_element(data, data.begin()+3); // same as above

    std::println("*(begin+3) == {}", *(data.begin()+3));

    // Because the algorithm also partitions the range
    // - 1,2,3 are guaranteed to be ordered before 4
    // - 5,6,8,9 are guaranteed to be ordered after 4

    std::println("data == {}", data);

    std::vector<std::string> labels{"dd", "bbbb", "aaaaa", "e", "xxx"};
    // Both versions support custom comparator
    // and the range version supports a projection
    std::ranges::nth_element(labels, // input range
        labels.begin()+2,            // pivot
        std::less<>{},               // comparator
        [](const std::string& s) {   // projection
            return s.length();
        });
    // *(labels.begin()+2) == "xxx" (median length string)

    std::println("*(begin+2) == {}", *(labels.begin()+2));
    std::println("labels == {}", labels);
}
```
Moving data efficiently (before C++23) can get tricky when we work with heterogeneous containers since move construction and move assignment are only supported by containers of the same type.

One viable option is using std::move_iterator and the iterator constructor. This will result in an element-wise move construction.
https://compiler-explorer.com/z/dhbE878P1

```c++

struct MyClass {};
std::vector<std::unique_ptr<MyClass>> get_elements();

int main() {
    std::vector<std::unique_ptr<MyClass>> src = get_elements();

    // Wouldn't compile: incompatible type
    // std::list<std::unique_ptr<MyClass>> err1(std::move(src));

    // Wouldn't compile: std::unique_ptr is not copyable
    // std::list<std::unique_ptr<MyClass>> err2(src.begin(), src.end());

    // OK
    std::list<std::unique_ptr<MyClass>> dst(
        std::move_iterator(src.begin()),
        std::move_iterator(src.end())
    );
}

std::vector<std::unique_ptr<MyClass>> get_elements() {
    std::vector<std::unique_ptr<MyClass>> result;
    result.emplace_back();
    result.emplace_back();
    result.emplace_back();
    return result;
}
```
The trio of boolean algorithms, std::all_of, std::any_of, and std::none_of, provide the corresponding boolean reductions on top of a unary predicate.

While the std::all_of and std::none_of return true for empty ranges, std::any_of requires a positive presence and will return false for an empty range.

The algorithms have both the C++17 parallel and C++20 range versions.
https://compiler-explorer.com/z/v8457oW77

```c++

int main() {
    std::vector<int> data{2, 2, 2, 4, 6, 8};
        
    auto is_even = [](int v) { return v % 2 == 0; };
    auto is_odd = [](int v) { return v % 2 != 0; };
    auto is_double_digit = [](int v) { return v > 9 && v < 100; };

    bool r1 = std::all_of(data.begin(), data.end(), is_even);
    // r1 == true

    std::println("r1 == {}", r1);

    bool r2 = std::none_of(data.begin(), data.end(), is_odd);
    // r2 == true

    std::println("r2 == {}", r2);

    bool r3 = std::any_of(data.begin(), data.end(), is_double_digit);
    // r3 == false

    std::println("r3 == {}", r3);

    std::vector<bool> flags{true, true, false};

    // If the elements are already convertible to bool, 
    // we can use std::identity{} instead of a predicate
    bool r4 = std::all_of(flags.begin(), flags.end(), std::identity{});
    // r4 == false

    std::println("r4 == {}", r4);

    struct Custom { int v; };
    std::vector<Custom> nested{{4},{2},{0}};

    // With a projection
    bool r5 = std::ranges::any_of(nested, is_even, &Custom::v);
    // r5 == true

    std::println("r5 == {}", r5);
}
```
The C++20 spaceship operator provides a three-way comparison, resulting in one of three ordering types.

std::strong_ordering for which equality implies that the values are indistinguishable, <br />std::weak_ordering, which allows equal, distinguishable values, and std::partial_ordering, which allows uncomparable values.
https://compiler-explorer.com/z/drqnqzKxa

```c++

struct Coord {
    int x;
    int y;
    friend auto operator<=>(const Coord&, const Coord&) = default;
};

struct Point : Coord {
    int magnitude() const {
        return x*x + y*y;
    }
    // int{}<=>int{} would produce std::strong_odering, 
    // we have to manually specify the ordering
    friend auto operator<=>(const Point& left, const Point& right) -> std::weak_ordering {
        return left.magnitude() <=> right.magnitude();
    }
    friend bool operator==(const Point&, const Point&) = default;
};

int main() {
    // Integral types are strongly ordered
    using cmp_int = decltype(0 <=> 1);
    // cmp_int == std::strong_ordering

    static_assert(std::is_same_v<cmp_int, std::strong_ordering>);

    // Aggregates formed from strongly ordered components end up 
    // also strongly ordered
    using cmp_agg = decltype(Coord{0,0} <=> Coord{1,1});
    // cmp_agg == std::strong_ordering

    static_assert(std::is_same_v<cmp_agg, std::strong_ordering>);

    // Comparing the magnitude of a coordinate (distance from [0,0])^2
    // provides only weak ordering, different coordinates have the same magnitude
    using cmp_mag = decltype(Point{0,1} <=> Point{1,0});
    // cmp_mag == std::weak_ordering

    static_assert(std::is_same_v<cmp_mag, std::weak_ordering>);

    // Notably floating-point numbers are only partially ordered:
    using cmp_flt = decltype(-0.0<=>0.0);
    // cmp_flt == std::partial_ordering

    static_assert(std::is_same_v<cmp_flt, std::partial_ordering>);

    bool r1 = -0.0 == 0.0;
    bool r2 = std::signbit(-0.0) == std::signbit(0.0);
    // r1 == true, r2 == false
    // -0.0 and 0.0 are equal, but distinguishable

    std::println("(-0.0 == 0.0) == {}, (std::signbit(-0.0) == std::signbit(0.0)) == {}", r1, r2);
 
    bool r3 = NAN == 0.0 || NAN < 0.0 || NAN > 0.0;
    // r3 == false, NaN is unordered

    std::println("(NAN == 0.0 || NAN < 0.0 || NAN > 0.0) == {}", r3);
}
```
The std::bitset is a statically sized container for storing bits.

The container offers bit-level access, boolean reductions (all, any, none), bitwise operations (AND, OR, XOR, NOT) with bitsets of the same size, bitshifts and state manipulations (set, reset, flip).

Since C++23, std::bitset is fully constexpr.
https://compiler-explorer.com/z/143P1rdrr

```c++

int main() {
    std::bitset<16> data;

    // the zero index refers to the least significant bit
    data[0] = 1;
    // data.to_ullong() == 1

    std::println("data.to_ullong()  == {:6} / 0b{:0>16b}", data.to_ullong(), data.to_ullong());

    // set all bits to 1
    data.set();
    // data.to_ullong() == 65535

    std::println("data.to_ullong()  == {:6} / 0b{:0>16b}", data.to_ullong(), data.to_ullong());

    // flip all bits (setting them to zero)
    data.flip();
    // data.to_ullong() == 0

    std::println("data.to_ullong()  == {:6} / 0b{:0>16b}", data.to_ullong(), data.to_ullong());

    data[0] = 1;
    data[2] = 1;
    data[4] = 1;

    std::println("data.to_ullong()  == {:6} / 0b{:0>16b}", data.to_ullong(), data.to_ullong());

    std::bitset<16> other;
    other[0] = 1;
    other[2] = 1;

    std::println("other.to_ullong() == {:6} / 0b{:0>16b}", other.to_ullong(), other.to_ullong());

    // bitwise AND
    data &= other;
    // data.to_ullong() == 5

    std::println("data.to_ullong()  == {:6} / 0b{:0>16b}", data.to_ullong(), data.to_ullong());

    // any_of
    bool test = data.any();
    // test == true

    std::println("test == {}", test);

    // count of 1 bits
    size_t cnt = data.count();
    // cnt == 2

    std::println("cnt == {}", cnt);

    // Does support comparison, but no ordering
    bool same = (data == other);
    // same == true

    std::println("same == {}", same);
}
```
When adding support for C++20 three-way comparison into a codebase you might need to bridge legacy code with new code that relies on operator&lt;=&gt;.

The three helper functions, std::compare_strong_order_fallback, std::compare_weak_order_fallback and std::compare_partial_order_fallback, will produce an ordering using either the operator&lt;=&gt; or old-style comparison functions.
https://compiler-explorer.com/z/s3b3z63Gr

```c++

// Type with support for old-style comparison
struct Coord {
    int x;
    int y;
    friend bool operator < (const Coord& left, const Coord& right) {
        if (left.x == right.x)
            return left.y < right.y;
        return left.x < right.x;
    }
    friend bool operator == (const Coord& left, const Coord& right) {
        return left.x == right.x && left.y == right.y;
    }
};

int main() {
    Coord a{1, 2}, b{2, 2};

    // Wouldn't compile, type doesn't implement operator<=>
    // auto v = a <=> b;

    // Produces strong_ordering using operator == and <
    auto s = std::compare_strong_order_fallback(a,b);
    // std::is_lt(s) == true
    // decltype(s) == std::strong_ordering

    std::println("std::is_lt(s) == {}", std::is_lt(s));
    static_assert(std::is_same_v<decltype(s), std::strong_ordering>);

    // Produces weak_ordering using operator == and <
    auto w = std::compare_weak_order_fallback(a,b);
    // std::is_lt(w) == true
    // decltype(w) == std::weak_ordering

    std::println("std::is_lt(w) == {}", std::is_lt(w));
    static_assert(std::is_same_v<decltype(w), std::weak_ordering>);

    // Produces partial_ordering using operator== and < (both a < b, b < a)
    auto p = std::compare_partial_order_fallback(a,b);
    // std::is_lt(p) == true
    // decltype(p) == std::partial_ordering

    std::println("std::is_lt(p) == {}", std::is_lt(p));
    static_assert(std::is_same_v<decltype(p), std::partial_ordering>);
}
```
The set of heap algorithms: std::make_heap, std::push_heap, std::pop_heap and std::sort_heap can be used as a replacement for std::priority_queue and std::set when it is desirable to keep the elements in a contiguous storage or when we require cheap element extraction.

For the benefits, we pay with a more error-prone interface.
https://compiler-explorer.com/z/rbdG63asn

```c++

int main() {
    std::vector<int> data{8,2,1,7,4,5,3,6,9};
    // initialize max-heap
    auto begin = data.begin(), end = data.end();
    std::make_heap(begin, end);

    // pop each element from the heap producing
    // a sorted order in the vector
    while (begin != end) {
        // pop_heap swaps the max element with 
        // the last element in range, maintaining the heap
        std::pop_heap(begin, end--);
        // iterate over 9,8,...
    }
    // data == {1, 2, 3, 4, 5, 6, 7, 8, 9}

    std::println("data == {}", data);

    std::vector<std::string> labels{"world","bye","fox","lazy","dog"};
    std::make_heap(labels.begin(), labels.end());
    // labels now in heap order

    std::println("labels == {}", labels);

    // extract element from heap, without removing it from the vector
    std::pop_heap(labels.begin(), labels.end());
    // in-place modify
    labels.back()[0] = 'e';
    // insert it back into the heap
    std::push_heap(labels.begin(), labels.end());

    std::sort_heap(labels.begin(), labels.end());
    // labels == {"bye", "dog", "eorld", "fox", "lazy"}

    std::println("labels == {}", labels);
}
```
Passing data around as C-style arrays might be unavoidable when interacting with legacy code or C APIs. This can lead to errors when keeping track of the array size.

C++20 introduced std::span, a view that can wrap a contiguous memory block in a range interface. This allows us to safely use algorithms and views to process data without copying it into a C++ container.
https://compiler-explorer.com/z/G9bjzEaPx

```c++

size_t read(char* buffer, size_t size);
int get_data(char** buffer, size_t* size);

int main() {
    char buffer[16];
    auto view = std::span(buffer);
    // Call C API using a wrapped buffer
    size_t cnt = read(view.data(), view.size());
    // Process a sub-view based on the number of actually read bytes
    for (auto v : view.subspan(0, cnt)) {
        std::print("{} ", v);
    }
    std::println("");

    char* buff;
    size_t length;
    int ret = get_data(&buff, &length);
    if (ret != 0) { return ret; }
    // Wrap a C buffer into a std::span
    auto buffer_view = std::span(buff, length);

    // std::span works as any other range, e.g. reverse iteration:
    for (auto it = buffer_view.rbegin(); it != buffer_view.rend(); ++it) {}
    // or combining with views:
    for (auto v : buffer_view | std::views::drop(8)) {
        std::print("{} ", v);
    }
    std::println("");

    struct Data {
        int x;
        int y;
    };

    std::vector<Data> data{{2,3},{1,5},{4,4}};
    // Acces the underlying representation:
    std::span<std::byte> bytes = std::as_writable_bytes(std::span(data));
    
    std::print("0x");
    for (auto v : bytes)
        std::print("{:x}", std::to_underlying(v));
    std::println("");
}

size_t read(char* buffer, size_t size) {
    size_t cnt = size >= 4 ? 4 : size;
    for (size_t i = 0; i < cnt; ++i)
        buffer[i] = 'a'+i;
    return cnt;
}

int get_data(char** buffer, size_t* size) {
    static char buff_[16]{'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p'};
    *buffer = buff_;
    *size = 16;
    return 0;
}
```
The min-max algorithms are relatively simple but offer multiple overloads suitable for different use cases.

In C++11, the std::minmax algorithm was added to std::min and std::max, and all three algorithms received an overload for std::initializer_list.

C++20 introduced corresponding versions in the std::ranges namespace and a new overload that operates on a range.
https://compiler-explorer.com/z/YGzovrK4x

```c++

int main() {
    int a = 20, b = 30;

    // Basic overloads return a const-reference to the minimum element.
    auto &r1 = std::min(a,b);
    // &r1 == &a

    std::println("&r1 == {}, &a == {}", (void*)&r1, (void*)&a);

    b = 20;
    // Worth noting that both min and max are biased 
    // towards the first argument.
    auto &r2 = std::min(a,b);
    auto &r3 = std::max(a,b);
    // &r2 == &r3 == &a

    std::println("&r2 == {}, &r3 == {}, &a == {}", (void*)&r2, (void*)&r3, (void*)&a);

    // C++11 introduced std::minmax
    a = 10;
    b = 5;
    // Returns a std::pair<const&,const&> which can be deconstructed
    // using C++17 structured binding.
    auto [min, max] = std::minmax(a,b);
    // &min == &b, &max == &a

    std::println("&min == {}, &max == {}, &a == {}, &b == {}", (void*)&min, (void*)&max, (void*)&a, (void*)&b);

    // Keep in mind that because std::minmax returns std::pair<const&,const&>
    // passing in temporaries will result in a dangling reference.
    auto q = std::minmax(30, a);
    // q.second is a dangling reference

    // C++11 also introduced initializer list overloads. These return 
    // by value due to the temporary nature of initializer lists.
    int v = std::min({5,2,4,3,9});
    // v == 2

    std::println("v == {}", v);

    // Even std::minmax returns by value, i.e. std::pair<int,int>
    auto [i, j] = std::minmax({5,2,4,3,9});
    // i == 2, j == 9

    std::println("i == {}, j == {}", i, j);

    // C++20 introduced a range variant.
    std::vector<int> data = {5,2,4,3,9};

    // Accepts any range and returns by value.
    auto [x,y] = std::ranges::minmax(data);
    // x == 2, y == 9

    std::println("x == {}, y == {}", x , y);
}
```
The std::iota is a simple algorithm that generates consecutive values by repeatedly applying the prefix increment, starting from the initial value.

While the base std::iota algorithm is fairly niche, the C++20 lazily evaluated std::views::iota is a more helpful version, particularly when combined with other views.
https://compiler-explorer.com/z/vso76jzjz

```c++

int main() {
    std::vector<int> data(5);
    std::iota(data.begin(), data.end(), 42);
    // data == {42, 43, 44, 45, 46}

    std::println("data == {}", data);

    // One convenient use case is to generate an "indirect" container.
    std::vector<std::vector<int>::iterator> indirect(data.size());
    std::iota(indirect.begin(), indirect.end(), data.begin());

    // Useful when swapping is expensive or impossible.
    // We can manipulate the indirect container instead.
    std::sort(indirect.begin(), indirect.end(), 
        [](auto& left, auto& right) {
            return *left > *right;
        });
    // data == {42, 43, 44, 45, 46}
    // indirect ~ {46, 45, 44, 43, 42}

    std::println("indirect ~ {}", indirect | std::views::transform([](auto it) { return *it; }));

    auto view1 = std::views::iota(9,12);
    // view1 = {9, 10, 11}

    std::println("view1 == {}", view1);

    auto view2 = std::views::iota(42);
    // view2 == {42, 43, ...}, Infinite view

    std::println("view2 ~ {}", view2 | std::views::take(5));

    // Infinite std::view::iota, zipped with a finite range
    // (std::views::zip is C++23)
    auto view3 = std::views::zip(std::views::iota(1), data);
    // view3 == {{1, 42}, {2, 43}, {3, 44}, {4, 45}, {5, 46}}

    std::println("view3 == {}", view3);
}
```
When we need to shuffle elements into a random order, we can use the C++11 std::shuffle algorithm, which relies on a random engine as the source of randomness.

Any random engine can be used, but for simple use cases, the implementation-defined std::default_random_engine is typically sufficient.
https://compiler-explorer.com/z/PzsW48Eq3

```c++

int main() {
    std::vector<int> data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};

    std::shuffle(
        data.begin(), data.end(), // all elements
        // source of randomness with a specified seed
        std::default_random_engine(42));
    // data == {2, 7, 4, 0, 6, 1, 8, 3, 5, 9} libstdc++
    // data == {6, 8, 7, 0, 1, 4, 9, 3, 2, 5} libc++

    std::println("{}", data);

    // Ranges variant with a random seed:
    std::random_device rd;
    std::ranges::shuffle(data,
        std::default_random_engine(rd()));
    // data now re-shuffled

    std::println("{}", data);
}
```
Opaque Enum Declaration is an enum declaration (specifying the name and the underlying type) that does not list the enumerators or their values.

This gives us the benefit of an enumeration, a strongly typed integral type without implicit conversion semantics (for scoped enumerations), without exposing the enumerator names or their values as part of the API.
https://compiler-explorer.com/z/h54d4zGv9

```c++
cmake_minimum_required(VERSION 3.5)

project(enum)

add_executable(scoped_enum
    library.cpp
    main.cpp)
```
The std::for_each algorithm invokes the provided invocable on each range element in order, ignoring the result. The invocable can be stateful, and the algorithm returns it in its final state.

While the range-based for-loop has mostly replaced the use cases for std::for_each, it still comes in handy as a trivial parallelization tool through the C++17 parallel version.
https://compiler-explorer.com/z/EP7TnKev3

```c++

void print_range(std::string_view name, auto&& rng) {
    std::string delim = "";
    std::print("{} == [", name);
    for (auto v : rng)
        std::print("{}{}", std::exchange(delim, ", "), v);
    std::println("]");
}

int main() {
    std::vector<int> data{1, 2, 3, 4, 5};
    // The invocable can mutate the elements
    std::for_each(data.begin(), data.end(), [](int& v) { ++v; });
    // data == {2, 3, 4, 5, 6}

    print_range("data", data);

    struct Stateful {
        int sum;
        void operator()(int& v) {
            sum += v;
            ++v;
        }
    };

    auto result = std::for_each(data.begin(), data.end(), Stateful{0});
    // data == {3, 4, 5, 6, 7}
    // result.sum == 20

    print_range("data", data);
    std::println("result.sum == {}", result.sum);

    struct Custom {};
    auto process = [](Custom&){};
    std::vector<Custom> rng(10,Custom{});
    // Parallel execution C++17
    std::for_each(std::execution::par_unseq, // parallel, in any order
        rng.begin(), rng.end(), // all elements
        process // invoke process on each element
    );

    std::vector<std::optional<int>> opt{{},2,{},4,{}};
    // Range version with projection C++20
    std::ranges::for_each(opt,
        [](int v) {
            // iterate over projected values
            // {0, 2, 0, 4, 0}
            std::print("{} ", v);
        },
        [](std::optional<int>& v){
            // projection that will return 
            // the contained value or zero
            return v.value_or(0);
        });
    std::println("");
}
```
The Monostate pattern (not to be confused with std::monostate) is a pattern with similar goals to a Singleton.

Where a Singleton only permits a single instance, the Monostate pattern can be instantiated many times while ensuring only one instance of its internal state.

A Monostate (with all the downsides of global state) can be a better fit for testability.
https://compiler-explorer.com/z/veE3vfo7P

```c++

struct MonoConfig {
  MonoConfig() {
    // ensure a single initialization outside of the static chain
    // if we don't need multi-threaded safety we can downgrade to
    // a boolean flag
    std::call_once(flag_, populate_config);
  }

  // Interface to acess the monostate
  uint64_t get_id() const { return field1; }
  const std::string& get_label() const { return field2; }

private:
  static std::once_flag flag_;
  static void populate_config() {
    /* read the fields from config source */
    field1 = UINT64_C(42);
    field2 = "Hello World";
  }
  static uint64_t field1;
  static std::string field2;
};

// All static members left default initialized
std::once_flag MonoConfig::flag_;
uint64_t MonoConfig::field1;
std::string MonoConfig::field2;


// When combined with the PIMPL pattern we can mock/fake
// the global state:
struct ImplIface {};
struct Actual : ImplIface {
  static std::unique_ptr<ImplIface> make() {
    return std::make_unique<Actual>();
  }
};
struct Testing : ImplIface {
  static std::unique_ptr<ImplIface> make() {
    return std::make_unique<Testing>();
  }
};

// Switch active type based on testing/production
using ActiveType = Testing;

struct MonoPIMPL {
  MonoPIMPL() {
    std::call_once(flag_, [] { impl_ = ActiveType::make(); });
  }
  /* expose ImplIface as any other PIMPL */
private:
  static std::once_flag flag_;
  static std::unique_ptr<ImplIface> impl_;
};

std::once_flag MonoPIMPL::flag_;
std::unique_ptr<ImplIface> MonoPIMPL::impl_;

int main() {
    // Create instance of the monostate object
    MonoConfig config;
    // to access the global state
    config.get_label();

    // Creating additional instances is a no-op.
    // Note that when stored as a member, it will still take up 
    // minimum 1 byte unless we use [[no_unique_address]].
    MonoConfig a, b, c, d, e, f, g, i, j, k;

    MonoPIMPL x;
}
```
When using std::swap with user-defined types, we need to be careful with the consequences of Argument Dependent Lookup.

To properly call std::swap, we have to pull the default implementation into the local scope before making an unqualified call.

Alternatively, with C++20, a qualified call to std::ranges::swap will always do the correct thing.
https://compiler-explorer.com/z/4nY1sK3Kn

```c++

namespace MyNamespace {
struct MyClass {
    // Use inline friend function to implement custom swap.
    friend void swap(MyClass&, MyClass&) {
        std::println("Custom implementation for MyClass");
    }
};

struct MyOtherClass {};
}

// Note that a non-function declaration with the name swap
// at global scope will be found during unqualified lookup
// and will prevent ADL.
// constexpr auto swap = "Hello!";

int main() {
    MyNamespace::MyClass a, b;
    MyNamespace::MyOtherClass x, y;

    std::println("Qualified calls:");

    // Fully qualified call, will always call std::swap
    std::swap(a,b); // calls std::swap
    std::swap(x,y); // calls std::swap

    std::println("\nUnqualified calls:");

    // No suitable swap for MyOtherClass.
    swap(a,b); // calls MyNamespace::swap
    // swap(x,y); // would not compile

    std::println("\nWith using std::swap:");

    // Pull std::swap as the default into local scope:
    {
    using std::swap;
    swap(a,b); // calls MyNamespace::swap
    swap(x,y); // calls std::swap
    }

    std::println("\nRanges:");

    // C++20 std::ranges::swap which will do the correct thing:
    std::ranges::swap(a,b); // calls MyNamespace::swap
    std::ranges::swap(x,y); // default swap
}
```
The C++17 std::variant is a type-safe alternative to a union.

Unlike a union, where accessing the non-active member is undefined behaviour, a std::variant will instead throw an exception.

A std::variant will also internally handle the correct construction and destruction of non-trivial members (which otherwise would have to be handled manually).
https://compiler-explorer.com/z/sMEj3177G

```c++

struct NoDefault{ NoDefault(int) {} };

int main() {
    std::variant<std::string, int, double> v;

    // Variant is never empty, without arguments it 
    // default constructs the first type:
    bool x = std::holds_alternative<std::string>(v);
    // x == true

    std::println("x == {}", x);

    // Assignment sets the active member:
    v = 10;
    // std::holds_alternative<int>(v) == true

    std::println("std::holds_alternative<int>(v) == {}", std::holds_alternative<int>(v));

    int y = std::get<int>(v);
    // OK, y == 10
    y = std::get<1>(v); // same as above, by index

    std::println("std::get<int>(v) == {}, std::get<1>(v) == {}", std::get<int>(v), std::get<1>(v));

    try {
        // throws when we attempt to access the wrong type
        double z = std::get<double>(v);
    } catch (const std::bad_variant_access& ex) {
        // ex.what() == std::get: wrong index for variant
        std::println("ex.what() == {}", ex.what());
    }

    // Because a variant can't be empty, to default construct,
    // the first type needs to be default constructible:
    // std::variant<NoDefault,int> i; // Wouldn't compile
    std::variant<NoDefault,int> j(10); // contains int
    std::variant<NoDefault,int> k(NoDefault{10}); // OK, contains NoDefault
}
```
When working with std::variant, processing the current active member can lead to cumbersome code.

An alternative, especially when the processing code is generic, is the std::visit.

The std::visit requires as an argument an invocable that is compatible with each of the contained types. This can be achieved through a combination of concrete and generic operator() overloads.
https://compiler-explorer.com/z/fMoMTjsrG

```c++

// Helper relying on CTAD. Inherits from constructor arguments 
// and exposes their call operator.
template <typename ...Ts>
struct overloaded : Ts... {
    using Ts::operator()...;
};

int main() {
    std::variant<int,double,std::string> v = "hello world!";

    // Generic visit, will instantiate the generic lambda for each type:
    std::visit([](auto&& x) {
        std::println("{}", x);
    }, v);
    // prints "hello world!"


    v = 2.4;
    // Create a new type by inheriting from lambdas that handle each type:
    std::visit(overloaded{
        [](int& x) {
            std::println("Contains 'int', value: {}", x);
        },
        [](double& x) {
            std::println("Contains 'double', value: {}", x);
        },
        [](std::string& x) {
            std::println("Contains 'std::string', value: {}", x);
        }
    }, v);
    // prints "Contains 'double', value: 2.4"

    std::println("");

    // Multiple variants can be visited, 
    // each mapping to a function argument:
    std::variant<std::string,int> a("hello world!");
    std::variant<double,char> b('X');

    std::visit([](auto&& first, auto&& second) {
        std::println("First argument:");
        std::println("\ttype == {}", typeid(first).name());
        std::println("\tvalue == {}", first);
        std::println("Second argument:");
        std::println("\ttype == {}", typeid(second).name());
        std::println("\tvalue == {}", second);
    }, a, b);
    // prints (type names are implementation specific):
    // First argument:
    //   type == NSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEEE
    //   value == hello world!
    // Second argument:
    //   type == c 
    //   value == X

    std::println("");

    std::variant<int,long long,float,double> x = 7.4;
    // The callables in visit can return, however, 
    // all overloads must return the same type

    // Wouldn't compile:
    // auto r1 = std::visit([](auto e) { return e; }, x);

    auto r1 = std::visit([](auto e) {
        return static_cast<int>(e);
    }, x);
    // r1 == 7, decltype(r1) == int

    static_assert(std::is_same_v<decltype(r1), int>);
    std::println("r1 == {}", r1);

    // C++20 added a version of visit that allows us to specify 
    // a common return type
    auto r2 = std::visit<int>([](auto e) {
        return e;
    }, x);
    // r2 == 7, decltype(r2) == int

    static_assert(std::is_same_v<decltype(r2), int>);
    std::println("r2 == {}", r2);
}
```
C++20 introduced default comparison operators.

The default spaceship comparison operator implements a piece-wise comparison, comparing the bases and members in order. The result is a strong, weak or partial comparison (based on the weakest sub-result).

The default spaceship operator will implicitly default the equality operator if none is declared.
https://compiler-explorer.com/z/foj6nTeh1

```c++

struct Custom {
    int v;
    // Default operator==, also provides operator!=
    friend bool operator==(const Custom&, const Custom&) = default;
    // Same as: bool operator==(const Custom&) const = default;
};

struct Point {
    int x;
    int y;   
    // Default three-way comparison, also defaults operator== if none is declared:
    friend auto operator<=>(const Point&, const Point&) = default;
    // Same as: auto operator <=>(const Point&) const = default;
};

struct Base {
    int id;
    auto operator<=>(const Base&) const = default;
};

struct Derived : Base {
    std::string data;
  
    // Default spaceship operator:
    auto operator<=> (const Derived&) const = default;
  
    /*  Manual implementation with the same semantics:
    auto operator<=>(const Derived& other) const {
        auto cmp = (const Base&)*this <=> (const Base&)other;
        if (std::is_neq(cmp)) // id != other.id
            return cmp;
        return this->data <=> other.data;
    }
    // We would also need to bring back operator ==
    auto operator==(const Derived&) == default;
    */
};

int main() {
    Custom a{4}, b{5};
    // a != b

    std::println("(a == b) == {}, (a != b) == {}", a == b, a != b);

    Point c{1,2}, d{1,3};
    // c < d, std::is_lt(c <=> d) == true

    std::println("(c < d) == {}, std::is_lt(c <=> d) == {}", c < d, std::is_lt(c <=> d));

    Derived e{2,"hello"}, f{2,"bye"};
    // e > f, std::is_gt(e <=> f) == true

    std::println("(e > f) == {}, std::is_gt(e <=> f) == {}", e > f, std::is_gt(e <=> f));
}
```
The transform-reduce is a common operation in distributed systems. C++17 added support for a namesake algorithm that offers the transform-reduce pattern for ranges.

The algorithm requires a commutative and associative reduction but offers both single and two-range variants and parallel overloads.
https://compiler-explorer.com/z/aYxcfT1Kn

```c++

int main() {
    std::vector<double> values{2.3, 9.1, 4.7, 7.1, 1.9, 5.2};
    std::vector<double> predictions{1.0, 2.0, 3.0, 4.0, 5.0, 6.0};

    // Calculate the mean square error:
    auto mse = std::transform_reduce(values.begin(), values.end(), // all elements
        predictions.begin(), // first element from second range
        0., // starting value, also dictates the type of accumulator
        std::plus<>{}, // reduction operation
        [](double val, double pred) { // transform operation
            return (val-pred)*(val-pred);
        }) / values.size(); // calculate the mean

    // Same, but computed in parallel.
    auto mse_par = std::transform_reduce(std::execution::par_unseq,
        values.begin(), values.end(),
        predictions.begin(), 
        0.,
        std::plus<>{},
        [](double val, double pred) {
            return (val-pred)*(val-pred); 
        }) / values.size();

    std::cout << "Mean square error == " << mse << "\n";
    std::cout << "Parellel version == " << mse_par << "\n";

    std::vector<int> data{1,2,3,4,5};
    // Unary (single-range) version is also supported:
    int sum_of_squares = std::transform_reduce(data.begin(), data.end(),
        0,
        std::plus<>{},
        [](int v) { return v*v; });
    std::cout << "Sum of squares == " << sum_of_squares << "\n";
}
```
Member functions in C++ can be cv-qualified, which allows the member function to be invoked on an object with corresponding cv-qualifiers.

Member functions can also be ref-qualified to distinguish between being invoked on an lvalue or rvalue object.

Providing a full set of qualified methods can introduce a lot of duplication; check C++23 &quot;deducing this&quot; for a fix.
https://compiler-explorer.com/z/Mf3zWW3fd

```c++

struct Qualified {
    void operation() { std::println("no qualifier"); }
    void operation() const { std::println("const"); }
    void operation() volatile { std::println("volatile"); }
    void operation() const volatile { std::println("const + volatile"); }
};

struct MyClass {
    void operation() & { std::println("mutable l-value"); }
    void operation() && { std::println("prvalue or xvalue"); }
    void operation() const& { std::println("immutable l-value"); }
    // combinations with volatile and const&& are also possible
};

int main() {
    Qualified x;
    x.operation(); // no qualifier

    std::as_const(x).operation(); // const

    volatile Qualified y;
    y.operation(); // volatile
    
    std::as_const(y).operation(); // const + volatile


    MyClass a;
    a.operation(); // const

    std::as_const(a).operation(); // non-const lvalue

    MyClass{}.operation(); // prvalue

    std::move(a).operation(); // xvalue
}
```
If we need selectively copy elements from one range to another, the standard offers the std::copy_if and std::remove_copy_if algorithms.

Both algorithms accept a predicate, copying elements for which the predicate returns true and false respectively.
https://compiler-explorer.com/z/bYnhf54G5

```c++

int main() {
    std::vector<int> data{1, 2, 3, 4, 5, 6, 7, 8};

    std::vector<int> dst1;
    std::copy_if(data.begin(), data.end(), // all elements
        std::back_inserter(dst1), // push_back elements into dst1
        [](int v) { return v % 2 == 0; }); // condition
    // dst1 == {2, 4, 6, 8}

    for (auto &v : dst1)
        std::cout << v << " ";
    std::cout << "\n";

    std::vector<int> dst2;
    std::remove_copy_if(data.begin(), data.end(), // all elements
        std::back_inserter(dst2), // push_back elements into dst2
        [](int v) { return v % 2 == 0; }); // negative condition
    // dst2 == {1, 3, 5, 7}

    for (auto &v : dst2)
        std::cout << v << " ";
    std::cout << "\n";
}
```
The std::chrono library added in C++11 enables simple time measurement.

The library provides two types for storing time information: time points and durations.

std::chrono supports the expected time manipulation operations, time literals (C++14), iostream and std::format formatting (C++20).
https://compiler-explorer.com/z/zvG5d4aMz

```c++

int main() {
    using namespace std::literals;  

    auto tp1 = std::chrono::steady_clock::now(); // obtain a time point
    std::this_thread::sleep_for(1ms); // millisecond literal
    auto tp2 = std::chrono::steady_clock::now();
    // base type and precision depend on the type of clock
    // decltype(tp2) == std::chrono::time_point<std::chrono::steady_clock>

    static_assert(std::is_same_v<decltype(tp1), std::chrono::time_point<std::chrono::steady_clock>>);

    /* The standard (pre-C++20) offers three clocks:
    - std::chrono::system_clock
    - std::chrono::steady_clock
    - std::chrono::high_resolution_clock
    */

    // Whether a clock is steady and its resolution can be queried using:
    bool system_is_steady = std::chrono::system_clock::is_steady;

    std::println("system_is_steady == {}", system_is_steady);

    using resolution = std::chrono::system_clock::duration;
    // resolution::period::num / resolution::period::den

    std::println("resolution == {}/{}\n", resolution::period::num, resolution::period::den);

    auto duration = tp2 - tp1; // difference of time points is a duration

    std::println("duration == {}\n", duration);
    // example output: 1115389ns

    // explicit type of duration, base type double, with micro resolution
    std::chrono::duration<double, std::micro> fpdur = tp2 - tp1;

    std::println("fpdur == {}\n", fpdur);
    // example output: 1115.39us

    // duractions can be converted between each other using duration_cast
    {
    using namespace std::chrono;
    auto milli = duration_cast<milliseconds>(tp2 - tp1);

    std::println("milli == {}\n", milli);
    // example output: 1ms
    }

    // When working with durations, keep in mind that they 
    // do not represent dates/calendar

    using day_t = std::chrono::duration<double, std::ratio<86400>>;
    // days, weeks, months, years were added in C++20

    // A year duration is the length of an average year
    day_t days_in_year = std::chrono::years{1};
    // days_in_year == 365.2425

    std::println("days_in_year == {:%Q%q}", days_in_year);
    
    // A month is 1/12 of a year
    day_t days_in_month = std::chrono::months{1};
    // days_in_month == days_in_year / 12 == 30.436875

    std::println("days_in_month == {:%Q%q}", days_in_month);
}
```
With C++20, the std::chrono library received a major update, introducing (among other things) support for representing dates.

An abstract (not necessarily valid) date can be represented using the std::chrono::year_month_day type, which supports arithmetic operations for years and months. When converted to system time, dates also support day arithmetic operations.
https://compiler-explorer.com/z/fKvfj6nY7

```c++

int main() {
    using namespace std::chrono;

    // Day in a year can be specified using literals and operator/
    auto moon_day = 2024y/July/20d;
    // decltype(moon_day) == std::chrono::year_month_day

    std::println("moon_day == {}", moon_day);
    static_assert(std::is_same_v<decltype(moon_day), std::chrono::year_month_day>);


    // std::chrono::year_month_day represents an abstract date
    // not rooted in a calendar
    auto weird_date = 2024y/June/44d;
    // decltype(weird_date) == std::chrono::year_month_day
    // weird_date.ok() == false

    std::println("weird_date == {}, weird_date.ok() == {}", weird_date, weird_date.ok());
    static_assert(std::is_same_v<decltype(weird_date), std::chrono::year_month_day>);

    // We can normalize a date to the system clock,
    // timezones will be covered in a separate post
    auto system_date = std::chrono::sys_days{weird_date};
    // decltype(system_date) == std::chrono::sys_days
    // system_date == 2024/07/14

    std::println("system_date == {}", system_date);
    static_assert(std::is_same_v<decltype(system_date), std::chrono::sys_days>);

    // std::chrono::sys_days can be converted to a weekday
    auto day = std::chrono::weekday{system_date};
    // day == std::chrono::Sunday

    std::println("day == {}", day);
    

    // std::chrono::sys_time represents the system time
    std::chrono::sys_time now = std::chrono::system_clock::now();
    // Alias for date + time
    std::chrono::sys_seconds time = std::chrono::floor<std::chrono::seconds>(now);
    // Alias for date only
    std::chrono::sys_days date = std::chrono::floor<std::chrono::days>(now);

    std::println("now == {}, time == {}, date == {}", now, time, date);

    
    // std::chrono::year_month_day supports artihmetic operations for years and months
    auto date1 = 2024y/May/1d;
    auto date2 = date1 + years{2};
    // date2 == 2026/05/01
    auto date3 = date1 + months{11};
    // date3 == 2025/04/01

    std::println("date1 == {}, date2 == {}, date3 == {}", date1, date2, date3);

    // For days, we need to normalize the date to sys_days
    for (auto date = 2024y/June/1d;
        date.month() == June;              // while in June
        date = sys_days{date} + days{1}) { // keep adding one day
        // iterate over all days in June 2024
        std::println("{}", date);
    }
}
```
One of the very useful features of the C++20 extension to std::chrono is support for relative &quot;pseudo-dates&quot;, such as the last day in May or the second Friday in June.

The library directly supports expressions for the last day and weekday of a month and expressions for the nth weekday of a month (e.g., the 2nd Sunday in June).
https://compiler-explorer.com/z/9To4sdf6z

```c++

int main() {
    using namespace std::chrono;

    // Last day in a month
    auto last_day_in_feb = February/last;
    // decltype(last_day_in_feb) == std::chrono::month_day_last

    std::println("last_day_in_feb == {}\n", last_day_in_feb);
    static_assert(std::is_same_v<decltype(last_day_in_feb), std::chrono::month_day_last>);

    // Find leaping years in 2024..2104
    for (auto year = 2024y; year <= 2104y; ++year) {
        auto maybe_leap = year/last_day_in_feb;
        // same as: year/Februrary/last
        // decltype(maybe_leap) == std::chrono::year_month_day_last

        static_assert(std::is_same_v<decltype(maybe_leap), std::chrono::year_month_day_last>);

        assert(maybe_leap.ok());
        if (maybe_leap.day() == 29d) { // requires maybe_leap.ok()
            std::println("{} is a leap year", year);
        }
    }
    std::println("");

    // Last weekday in a month
    auto last_sunday = December/Sunday[last];
    // decltype(last_sunday) == std::chrono::month_weekday_last

    static_assert(std::is_same_v<decltype(last_sunday), std::chrono::month_weekday_last>);

    // Iterate over the last Sundays in 2024..2030
    for (auto year = 2024y; year <= 2030y; ++year) {
        auto pseudo = year/last_sunday;
        // same as: year/December/Sunday[last];
        // decltype(pseudo) == std::chrono::year_month_weekday_last

        static_assert(std::is_same_v<decltype(pseudo), std::chrono::year_month_weekday_last>);

        // Convert to the actual year_month_day
        std::chrono::year_month_day actual{pseudo};
        
        std::println("Last Sunday in {} is {}", year, actual);
    }
    std::println("");

    // nth weekday in a month
    // US Thanksgiving date, 4th Thursday in November
    auto thanksgiving = November/Thursday[4]; // ordinal, not index
    // decltype(thanksgiving) == std::chrono::month_weekday

    static_assert(std::is_same_v<decltype(thanksgiving), std::chrono::month_weekday>);

    for (auto year = 2024y; year <= 2030y; year++) {
        // As long as the expression is not ambiguous, the order doesn't matter
        year_month_day date{thanksgiving/year};

        std::println("US thanksgiving in {} is {}", year, date);
    }
}
```
Time zones are a particularly annoying problem when dealing with time. The C++20 extension to std::chrono also introduced support for time zones.

The library now also supports unzoned time in the form of std::chrono::local_time. Unzoned time can be combined with a specific time zone to produce a std::chrono::zoned_time, which can then be converted to other time zones.
https://compiler-explorer.com/z/9beq1rcsb

```c++

int main() {
    using namespace std::chrono;

    // Time zone database version
    std::string tzdb_version = std::chrono::remote_version();

    std::println("Zone database version: {}", tzdb_version);

    // Iterate over valid timezones
    for (auto& zone : get_tzdb().zones) {
        std::print("{}, ", zone.name());
    }
    std::println("\n");

    // USA switch to summer time
    std::chrono::year_month_day usa_summer{2025y/March/Sunday[2]};
    // Europe switch to summer time
    std::chrono::year_month_day eu_summer{2025y/March/Sunday[last]};

    std::println("USA switching to summer time on {}", usa_summer);
    std::println("Europe switching to summer time on {}\n", eu_summer);

    // Locate two time zones in the time zone db that we will be working with
    auto prague = std::chrono::locate_zone("Europe/Prague");
    auto newyork = std::chrono::locate_zone("America/New_York");

    // Let's simulate a weekly meeting on Wednesday 15:00 Prague time

    // Local time is unzoned
    std::chrono::local_time meeting{local_days{2025y/March/Wednesday[1]}};

    // Iterate over all meetings until summer time change
    while (meeting < local_days{2025y/April/Sunday[1]}) {
        // create zoned time for Prague
        zoned_time<seconds> local{prague, local_days{meeting} + 15h};
        // create zoned time for NewYork from the Prague zoned time
        zoned_time<seconds> remote{newyork, local};

        std::println("{}: {}", prague->name(), local);
        std::println("{}: {}\n", newyork->name(), remote);

        meeting += weeks{1};
    }
}
```
The C++23 added the std::spanstream, effectively a std::stringstream equivalent operating on a borrowed contiguous range.

std::spanstream can be used to directly parse text stored in raw memory, string_views or string literals.

When using std::spanstream for writing, the std::spanstream will write as much output as fits into the output span, which can be inconvenient.
https://compiler-explorer.com/z/occYKs4d5

```c++

int main() {
    // Works as a stringstream, but with a borrowed underlying range

    // Parsing of text data in buffers, string_views, string literals
    char buffer[] = "10 20 30";
    std::spanstream stream(buffer);
    int a, b, c;
    stream >> a >> b >> c;
    // a == 10, b == 20, c == 30, stream.good() == true

    std::cout << a << " " << b << " " << c << "\n";
    std::cout << std::boolalpha << stream.good() << "\n";

    // Also works for output
    std::vector<char> data(1024*8,'\0');
    std::spanstream out(data);
    out << 10 << " " << 20 << " " << 30;
    // data == "10 20 30" (followed by nul characters)

    std::cout << std::quoted(data.data()) << "\n";

    // However note that handling write errors is cumbersome
    std::vector<char> limited(8, '\0');
    std::spanstream sstr(limited);
    sstr << 1000 << " ";

    auto pos = sstr.tellp(); // remember the offset
    sstr << 1000; // this fails (not enough space)
    // data == "1000 100"
    if (!sstr.good()) {
        sstr.clear(); // clear the error flags
        sstr.seekp(pos); // seek back
    }
    while (sstr << 'X'); // fill the rest of the buffer with 'X'
    // data == "1000 XXX"
    
    std::cout << "\"";
    for (auto c : limited) {
        std::cout << c;
    }
    std::cout << "\"\n";
}
```
The C++23 std::views::stride is a view that contains every n-th element from the source view.

This is useful when we want to represent column-order traversal using views.
https://compiler-explorer.com/z/1Mnn56fac

```c++
using namespace std::literals;

int main() {

auto text = "Hello   World!"sv;

// Iterate over every second element:
for (auto c : text | std::views::stride(2))
    std::cout << c << " ";
std::cout << "\n";

// Same but shifted by one by dropping the first element:
for (auto c : text | std::views::drop(1) | std::views::stride(2))
    std::cout << " " << c;
std::cout << "\n";
/* Prints:
H l o   W r d 
 e l     o l !
*/

std::cout << "\n";

std::vector<int> data{1,2,3,4,5,6,7,8,9};

// Treat data as a 3x3 2D array and do column-order traversal:
for (auto column : std::views::iota(0,3)) {
    for (auto e : data | std::views::drop(column) | std::views::stride(3))
        std::cout << e << " ";
    std::cout << "\n";
}
/* Prints:
1 4 7
2 5 8
3 6 9
*/

}
```
A typical pattern with legacy APIs is for the initializing function to return an opaque pointer that needs to be freed by manually calling a cleanup function.

std::unique_ptr can serve as a helper here, as it accepts custom deleters. Unfortunately, the interface is not ergonomic, which we can fix by introducing a short helper.
https://compiler-explorer.com/z/Gvr7ecPac

```c++

template <typename P, typename D>
auto wrap_unique(P* pointer, D&& deleter) {
    return std::unique_ptr<P, D>{
        pointer,
        std::forward<D>(deleter)
    };
}

int main() {
    auto file = wrap_unique(
        fopen("/dev/null", "r"),
        [](std::FILE* fp) noexcept {
            fclose(fp);
        });
    // file will autoclose when going out of scope
}
```
The std::partial_sort_copy is an unusual sorting algorithm. It doesn&#39;t require the source range to be random-access while providing O(n*logk) runtime complexity.

The algorithm will &quot;copy&quot; the top k elements to the output range (which is required to be random access) in sorted order.

Both C++17 parallel and C++20 range versions are available.
https://compiler-explorer.com/z/8ETMY6bzb

```c++

int main() {
    std::forward_list<std::string> names{"Emma", "Liam", "Zara", "Ethan", "Aria", "Mateo",
        "Ivy", "Finn", "Luna", "Kai", "Mila", "Oscar", "Ruby", "Levi", "Nora"};

    // The size of the output range determines the number 
    // of copied/sorted elements
    std::vector<std::string> out1(3);

    std::partial_sort_copy(
        names.begin(), names.end(), // source range
        out1.begin(), out1.end());  // destination range
    // out1 == {"Aria", "Emma", "Ethan"}

    std::println("out1 == {}", out1);

    std::vector<std::string> out2(5);

    std::partial_sort_copy(
        names.begin(), names.end(), // source range
        out2.begin(), out2.end(),   // destination range
        std::greater<>{});          // custom comparator
    // out2 == {"Zara", "Ruby", "Oscar", "Nora", "Mila"}

    std::println("out2 == {}", out2);

    std::vector<std::string> out3(4);

    // Range version
    auto length = [](const std::string& s) { return s.length(); };
    std::ranges::partial_sort_copy(names, out3, // ranges
        std::greater<>{}, // custom comparator
        length,           // projection for source range
        length);          // projection for destination range
    // out3 == {"Oscar", "Mateo", "Ethan", "Zara"}

    std::println("out3 == {}", out3);

    // Because of separate projections the destination range
    // can use a different element type
    std::vector<std::string_view> out4(4); // string views into the source range

    auto view_len = [](const std::string_view& s) { return s.length(); };
    std::ranges::partial_sort_copy(names, out4, // ranges
        std::greater<>{}, // comparator supports std::string/std::string_view comparison without conversions
        length,           // projection for std::string
        view_len);        // projection for std::string_view
    // out4 == {"Oscar", "Mateo", "Ethan", "Zara"}

    std::println("out4 == {}", out4);
}
```
C++11 introduced user-defined literals, allowing libraries to expose strongly typed unit value types in a natural and readable way.

Note that the only permitted base types for user-defined literals are unsigned long long int, long double, character types and string literal types.
https://compiler-explorer.com/z/f8TvhcPbe

```c++

namespace units {
    struct m {
        unsigned long long int value;
        friend auto operator<=>(const m&, const m&) = default;
    };
    struct km {
        unsigned long long int value;
        operator m() const { return {value*1000}; }
        friend auto operator<=>(const km&, const km&) = default;
    };
}

constexpr units::km operator"" _km(unsigned long long int v) { return {v}; }
constexpr units::m operator"" _m(unsigned long long int v) { return {v}; }

int main() {
    if (100_m < 1_km) {
        std::println("100m is less than 1km");
    }
    if (1001_m > 1_km) {
        std::println("1001m is more than 1km");
    }
}
```
The C++23 std::views::chunk_by is a view similar to C++20 std::views::split; however, unlike split, it operates using a binary predicate.

A new chunk will be started between the two tested elements when the predicate returns false.
https://compiler-explorer.com/z/Mvzo874s1

```c++

int main() {
    std::vector<int> data{1,2,-4,-2,-1,8,7,3,4,-5,-5};

    auto same_sign = [](int left, int right){
        return std::signbit(left) == std::signbit(right);
    };

    for (const auto& chunk : data | std::views::chunk_by(same_sign)) {
        // Iterate over chunks:
        // {1,2}, {-4,-2,-1}, {8,7,3,4}, {-5,-5}
        std::ranges::copy(chunk, std::ostream_iterator<int>(std::cout, " "));
        std::cout << "\n";
    }
}
```
Only a few algorithms in the standard library offer counted variants that operate using a begin iterator and the number of elements.

C++20 introduced the std::counted_iterator adapter that can turn any range version of an algorithm into a counted variant.
https://compiler-explorer.com/z/a99Tqev48

```c++

int main() {
    std::vector<int> data{1, 2, 3, 4, 5, 6, 7, 8, 9, 0};

    // A counted variant of std::copy.
    std::copy_n(data.begin(), 5, 
        std::ostream_iterator<int>(std::cout," "));
    std::cout << "\n";

    std::ranges::copy(
        // Adapt iterator, and specify number of elements.
        std::counted_iterator(data.begin(), 5), 
        // counted_iterator == default_sentinel if count is zero
        std::default_sentinel,
        std::ostream_iterator<int>(std::cout," "));
    std::cout << "\n";
}
```
The C++23 std::views::slide is a view over all positions of a sliding window of the specified size over the wrapped range.

I.e. the i-th element of std::views::slide of size n will be the subrange of [i, i+n) elements from the source range.
https://compiler-explorer.com/z/er7xhbnq4

```c++

int main() {
    std::vector<int> data{1,2,3,4,5,6,7,8,9};
    for (auto window : data | std::views::slide(4)) {
        // Iterate over all 4-element window positions:
        // {1,2,3,4}
        // {2,3,4,5}
        // {3,4,5,6}
        // {4,5,6,7}
        // {5,6,7,8}
        // {6,7,8,9}
        std::ranges::copy(window,
            std::ostream_iterator<int>(std::cout, " "));
        std::cout << "\n";
    }
}
```
The C++14 std::exchange is a simple utility that sets the first argument to the provided value and returns the original value.

While straightforward, this behaviour greatly simplifies the typical implementation of move-semantics and other use-cases that would otherwise have to rely on temporary variables.
https://compiler-explorer.com/z/M5sMGMfnn

```c++

// std::exchange as helper for move semantics
struct EraseOnMove {
    EraseOnMove(int value) : value_(value) {}
    EraseOnMove(EraseOnMove&& other) : value_(std::exchange(other.value_, 0)) {}
    EraseOnMove& operator=(EraseOnMove&& other) {
        // safe for this == &other
        value_ = std::exchange(other.value_, 0);
        /* Note: the following isn't safe for this == &other
            value_ = std::move(other.value_);
            other.value_ = 0;
        */
        return *this;
    }
    int value_;
};

// Fibonacci sequence using std::exchange
std::generator<int> fibonacci(size_t cnt) {
    int a = 0;
    int b = 1;
    while (cnt > 0) {
        co_yield std::exchange(a, std::exchange(b, a+b));
        /* same as:
        int b_old = b;
        b = a + b;
        int a_old = a;
        a = b_old;
        co_yield a_old;
        */
        --cnt;
    }
}

int main() {
    EraseOnMove a{10};
    EraseOnMove b(std::move(a));
    // a == {0}, b == {10}

    std::cout << "a == {" << a.value_ << "}, b == {" << b.value_ << "}\n";

    // std::exchange as helper for delimiting output
    std::string delim = "";
    for (int f : fibonacci(10))
        std::cout << std::exchange(delim,", ") << f;
    std::cout << "\n";
}
```
The C++23 std::views::as_rvalue is the view equivalent of the C++11 iterator adaptor std::move_iterator.

The elements of the view are presented as rvalues, which is useful when the source data is can be consumed, thus avoiding unnecessary copies.
https://compiler-explorer.com/z/b8d15eG6P

```c++

struct Tattler {
    Tattler() { std::clog << "Tattler()\n"; }
    Tattler(const Tattler&) { std::clog << "Tattler(const Tattler&)\n"; }
    Tattler(Tattler&&) { std::clog << "Tattler(Tattler&&)\n"; }
    Tattler& operator=(const Tattler&) { std::clog << "operator=(const Tattler&)\n"; return *this; }
    Tattler& operator=(Tattler&&) { std::clog << "operator=(Tattler&&)\n"; return *this; }
    ~Tattler() { std::clog << "~Tattler()\n"; }
};

int main() {
    std::vector<Tattler> data{{},{},{}};

    std::clog << "\nCopy iteration:\n";
    for (const auto& v : data | 
        std::views::transform([](const auto& e) {
            return e;
        })) { }
    // 3x copy constructor

    std::clog << "\nMove iteration:\n";
    for (auto v : data | 
        std::views::as_rvalue | 
        std::views::transform([](auto&& e) {
            return std::forward<decltype(e)>(e); 
        })) { }
    // 3x move constructor
    
    std::clog << "\nCleanup:\n";
}
```
C++23 addressed a long-standing problem with function objects.

Because the call operator() was not allowed to be a static method, function objects couldn&#39;t be converted to function pointers, and each call to function object incurred the performance penalty of passing the &quot;this&quot; pointer.

With C++23, the operator() is allowed to be static, removing both issues.
https://compiler-explorer.com/z/hc67zh7jv

```c++

namespace mynamespace {
struct oldstyle_fn {
    // normal method (incurs the penalty of passing this pointer)
    // both in the function and on the call site
    auto operator()(auto&& v) const {
        return std::forward<decltype(v)>(v);
    }
};

struct newstyle_fn {
    // static method
    static auto operator()(auto&& v) {
        return std::forward<decltype(v)>(v);
    }
};

constexpr inline auto oldstyle = oldstyle_fn{};
constexpr inline auto newstyle = newstyle_fn{};
}

int main() {
    // will pass both &oldstyle and 10
    int x = mynamespace::oldstyle(10);
    // will pass only 10
    int y = mynamespace::newstyle(10);
 

    {
    using namespace mynamespace;
    
    // Wouldn't compile
    // cannot convert a member function to a function pointer
    // int (*p1)(int&&) = oldstyle_fn::operator();

    // OK: member function -> member function pointer
    int (oldstyle_fn::*p2)(int&&) const = &oldstyle_fn::operator();
    (oldstyle.*p2)(10); // OK

    // OK: static member function -> function pointer
    int (*p3)(int&&) = newstyle_fn::operator();
    p3(10); // OK
    }
}
```
Preallocating capacity for elements is generally the performance-optimal approach. However, it adds code complexity, and we might not know the number of elements upfront.

Inserter adapters solve this problem by adapting the destination range and calling push_back (back_inserter), push_front (front_inserter) or insert (inserter) on each write.
https://compiler-explorer.com/z/Eb9ed4hrs

```c++

int main() {
    std::vector<int> src{1, 2, 3};
    std::list<int> dst{4};

    std::copy(src.begin(), src.end(), std::back_inserter(dst));
    // dst == {4, 1, 2, 3}

    for (auto v : dst)
        std::cout << v << " ";
    std::cout << "\n";

    std::copy(src.begin(), src.end(), std::front_inserter(dst));
    // dst = {3, 2, 1, 4, 1, 2, 3}

    for (auto v : dst)
        std::cout << v << " ";
    std::cout << "\n";

    std::copy(src.begin(), src.end(), 
        std::inserter(dst, std::next(dst.begin())));
    // dst = {3, 1, 2, 3, 2, 1, 4, 1, 2, 3}
    
    // Note that inserter should not be used with destination
    // ranges that invalidate iterators on insertion.

    for (auto v : dst)
        std::cout << v << " ";
    std::cout << "\n";
}
```
If we want to represent an optional value, we can use std::unique_ptr (C++11) and heap allocation (nullptr denoting absence). However, this incurs a runtime cost and can fail (and throw).

std::optional (C++17) will not allocate dynamic memory and only incurs the cost of storing a boolean.
https://compiler-explorer.com/z/sqf9ro1cE

```c++

struct Data{};

std::unique_ptr<Data> dynamic_return() {
    return std::make_unique<Data>();
}

std::optional<Data> no_allocation() {
    return std::make_optional<Data>();
}

int main() {
    auto x = dynamic_return();
    if (x != nullptr) { // or simply if(x)
        // process data
    }

    auto y = no_allocation();
    if (y.has_value()) { // or simply if(y)
        // process data
    }
}
```
Integer literals in C++ can be surprisingly complex if you care about their type. The type is determined by the combination of the specified suffix (if any), the base (e.g., decimal), and the literal&#39;s value.

An alternative to suffixes are macros from the cstdint header that will annotate a literal to map to the corresponding std::(u)int_leastXX_t type.
https://compiler-explorer.com/z/W43z495Gr

```c++

int main() {
    // The lowest ranked type is selected based on the size
    // the actual type is platform specific

    // for decimal base, and no suffix, the type is signed
    auto a1 = 0;
    // decltype(a1) == int

    static_assert(std::is_same_v<decltype(a1), int>);

    auto a2 = 3'000'000'000;
    // decltype(a2) == long

    static_assert(std::is_same_v<decltype(a2), long>);

    // 'u' suffix forces an unsigned type
    auto a3 = 0u;
    // decltype(a3) == unsigned

    static_assert(std::is_same_v<decltype(a3), unsigned>);

    auto a4 = 5'000'000'000u;
    // decltype(a4) == unsigned long

    static_assert(std::is_same_v<decltype(a4), unsigned long>);

    // for non-decimal, unsigned types can be selected 
    // even without the 'u' suffix
    auto a5 = 0x8000'0000; // INT_MAX + 1
    // decltype(a5) == unsigned

    static_assert(std::is_same_v<decltype(a5), unsigned>);

    // Size suffixes

    // Signed integer literals
    auto b1 = 0;
    // decltype(b1) == int

    static_assert(std::is_same_v<decltype(b1), int>);

    auto b2 = 0L; // at least long
    // decltype(b2) == long

    static_assert(std::is_same_v<decltype(b2), long int>);

    auto b3 = 0LL; // C++11, at least long long
    // decltype(b3) == long long

    static_assert(std::is_same_v<decltype(b3), long long int>);

    auto b4 = 0Z; // C++23, guaranteed std::ptrdiff_t
    // decltype(b4) == std::ptrdiff_t

    static_assert(std::is_same_v<decltype(b4), std::ptrdiff_t>);

    // Unsigned integer literals
    auto c1 = 0U; // unsigned type
    // decltype(c1) == unsigned

    static_assert(std::is_same_v<decltype(c1), unsigned int>);

    auto c2 = 0UL; // unsigned type, at least long
    // decltype(c2) == unsigned long

    static_assert(std::is_same_v<decltype(0UL), unsigned long int>);

    auto c3 = 0ULL; // C++11, unsigned type, at least long long
    // decltype(c3) == unsigned long long

    static_assert(std::is_same_v<decltype(0ULL), unsigned long long int>);

    auto c4 = 0UZ; // C++23, guaranteed std::size_t
    // decltype(c4) == std::size_t

    static_assert(std::is_same_v<decltype(c4), std::size_t>);

    // Fixed-sized integer type macro from <cstdint>
    auto d1 = UINT64_C(0); // C++11, guaranteed std::uint_least64_t
    // decltype(d1) == std::uint_least64_t

    static_assert(std::is_same_v<decltype(d1), std::uint_least64_t>);
}
```
The C++20 std::views::join will produce a view over the elements of sub-ranges. Effectively joining the sub-ranges into a single range.
https://compiler-explorer.com/z/ThbxEKMaj

```c++

int main() {
    std::vector<std::vector<int>> data{{1,2,3},{4,5,6},{7,8,9}};

    // Simple join over the 2nd dimension of the array
    auto view1 = data | std::views::join;
    // view1 == {1, 2, 3, 4, 5, 6, 7, 8, 9}

    for (auto v : view1)
        std::cout << v << " ";
    std::cout << "\n";

    // Join with a filter, skipping sub-ranges with odd sum
    auto view2 = data | std::views::filter([](const auto& rng) {
            return std::accumulate(rng.begin(), rng.end(), 0) % 2 == 0;
        }) | std::views::join;
    // view2 == {1, 2, 3, 7, 8, 9}

    for (auto v : view2)
        std::cout << v << " ";
    std::cout << "\n";
}
```
When implementing coroutines, we must decide how we want to handle exceptions.<br />One option is to ignore exceptions and have the unhandled_exception() method terminate.

Alternatively, we can use the C++11 exception storing and re-throwing support:

- std::current_exception<br />- std::exception_ptr<br />- std::rethrow_exception
https://compiler-explorer.com/z/h7jPnnYbE

```c++

template <typename Result>
struct Promise;

// Result type
template <typename Result>
struct Task : std::coroutine_handle<Promise<Result>> {
    using promise_type = Promise<Result>;

    ~Task() { this->destroy(); }
    
    Result& get_value() {
    	// if the coroutine ended with exception, rethrow
        if (this->promise().exception_)
            std::rethrow_exception(this->promise().exception_);
        // otherwise, return the result
        return this->promise().value_;
    }
};

// Promise type
template <typename Result>
struct Promise {
    Task<Result> get_return_object() { 
    	return {Task<Result>::from_promise(*this)}; 
    }
    std::suspend_always initial_suspend() noexcept { return {}; }
    std::suspend_always final_suspend() noexcept { return {}; }

    void return_value(auto&& val) { // store the result
        value_ = std::forward<decltype(val)>(val);
    }
    void unhandled_exception() { // store the exception
        exception_ = std::current_exception();
    }
    
    Result value_;
    std::exception_ptr exception_;
};

Task<int> coro_success() {
    co_return 1;
}

Task<int> coro_throws() {
    throw std::runtime_error("Failed.");
    co_return 2;
}

int main() {
    try {
        auto c1 = coro_success();
        c1.resume(); // let the coroutine run
        std::cout << c1.get_value() << "\n";
        // prints "1"  

        auto c2 = coro_throws();
        c2.resume(); // let the coroutine run
        std::cout << c2.get_value() << "\n";
        // c2.get_value() rethrows the exception
    } catch (const std::exception& e) {
        // e.what() == "Failed."
        std::cout << e.what() << "\n";
    }
}
```
The C++23 std::views::chunk produces a view of sub-ranges spanning the provided number of elements each.

If the source range cannot be divided without a remainder, the last element in the range will be a shorter subrange that contains the remainder of the elements.

The view works for input ranges and will provide additional features based on the type of the underlying range.
https://compiler-explorer.com/z/E5KTses7f

```c++

void print_range(auto&& rng);

int main() {
    std::vector<int> data{1,2,3,4,5,6,7,8,9};

    // Iterate over chunks of size 4 (and one chunk of size 1)
    for (const auto& chunk : data | std::views::chunk(4)) {
        print_range(chunk);
    }
    std::cout << '\n';

    auto view = data | std::views::chunk(4);
    auto second = view[1]; // OK
    // operator[] provided if source range is random access
    // second == {5,6,7,8}

    print_range(second);
    std::cout << '\n';

    auto last = view.back(); // OK
    // back() provided if source range is bidirectional
    // last == {9}

    print_range(last);
    std::cout << '\n';

    std::vector<int> cube(27,0);
    // Can be used to iterate multi-dimensional data stored as 1D
    for (const auto &slice : cube | std::views::chunk(3*3)) {
        for (const auto &row : slice | std::views::chunk(3)) {
            print_range(row);
        }
        std::cout << '\n';
    }
}

void print_range(auto&& rng) {
    std::cout << "{";
    std::string delim;
    for (int v : rng)
        std::cout << std::exchange(delim,", ") << v;
    std::cout << "}\n";
}
```
The C++11 inline namespaces are invisible to code but affect symbol names.

This makes them a great tool for versioning symbols when introducing ABI-breaking changes.
https://godbolt.org/z/41W6Mej9f

```c++

namespace Library {
    inline namespace v2 {
        void function();
    }
    // implicit "using namespace v2;"
}

int main() {
    Library::function(); // Will use Library::v2::function();
    // prints: "Hello World! v2"
}

// Library implementation:

// Older versions can be kept around (if possible), keeping binary compatibility.
// If removed, compiling against old header with new binary will result in linker error.
namespace Library {
    inline namespace v1 {
        void function() {
            std::cout << "Hello World! v1\n";
        }
    }
    inline namespace v2 {
        void function() {
            std::cout << "Hello World! v2\n";
        }
    }
}
```
The std::optional (C++17) offers several interfaces to access the contained value.

The choice depends on your coding style and your desired exception semantics.
https://godbolt.org/z/aea9c3KWr

```c++

struct Data {
    void method() {}
    int v;
};

int main() {
    std::optional<Data> x{Data{1}};

    // Pointer-style noexcept interface
    if (x != std::nullopt) {
        x->method();
        auto y = *x;
        // decltype(y) == Data
        // y.v == 1

        std::cout << "y.v == " << y.v << "\n";
    }

    // Explicit throwing interface
    if (x.has_value()) {
        auto y = x.value(); // throws bad_optional_access if empty
        // decltype(y) == Data
        // y.v == 1

        std::cout << "y.v == " << y.v << "\n";
    }

    // Default-value interface, throws only if construction of the contained type throws
    auto y = x.value_or(Data{2});
    // y.v == 1

    std::cout << "y.v == " << y.v << "\n";

    x = std::nullopt; // or x.reset();
    auto z = x.value_or(Data{2});
    // z.v == 2

    std::cout << "z.v == " << z.v << "\n";
}
```
C++17 introduced the [[nodiscard]] attribute that triggers a compiler warning when the result of a function call is discarded.

At a minimum, this attribute should be used for functions that are expensive to run and query functions that might be confused with their action counterparts.
https://compiler-explorer.com/z/TMMojeKcd

```c++
struct MyStruct {
    struct ExpensiveResult {};
    [[nodiscard]] ExpensiveResult expensive_call() { return {}; }
};

struct CustomVector {
    // C++20 - optional string that will be included in the error
    [[nodiscard("Did you mean to call clear()?")]]
    bool empty() const { return false; }
};


int main() {
    MyStruct x;
    x.expensive_call();

    CustomVector y;
    y.empty();
}
```
The std::transform algorithm can copy elements from one range to another, applying a transformation to each element. The transformation can change the type of the range.

The algorithm also provides a binary variant which can be used to reduce two ranges into one.

Both variants offer C++17 parallel and C++20 range versions.
https://compiler-explorer.com/z/9E8fq6EP3

```c++

int main() {
    // Copy with a transformation:
    std::vector<int> in{1, 2, 3, 4, 5, 6, 7};
    std::vector<double> out;

    std::transform(in.begin(), in.end(),
        std::back_inserter(out),
        [](int v) { return static_cast<double>(v)/2; });
    // out ~= {0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5}

    std::println("out ~= {}", out);


    // Reduce two ranges into one:
    std::vector<int> in1{1, 2, 3, 4, 5};
    std::vector<int> in2{0, 1, 0, 1, 0};
    std::vector<int> reduction;

    std::transform(in1.begin(), in1.end(), // first range
        in2.begin(), // second range, number of elements from first range
        std::back_inserter(reduction),
        [](int e1, int e2) { return e1*e2; });
    // reduction == {0, 2, 0, 4, 0}

    std::println("reduction == {}", reduction);


    // The output range can be one of the input ranges:
    std::string str = "welcome to somewhere";
    std::transform(str.begin(), str.end(), // input range
        str.begin(), // Note, can't be offset (only r1.begin(), r2.begin())
        [](char c) { return std::toupper(c); });
    // str == "WELCOME TO SOMEWHERE"

    std::println("str == {}", str);


    // Take care when working with expensive to copy types.
    struct Expensive{};
    std::vector<Expensive> data(10);
    std::transform(data.begin(), data.end(),
        data.begin(),
        [](const Expensive& v) { return v; }); // copy
    // To modify element in-place, use std::for_each or a range-for.


    // With the range version we can decompose 
    // the transformation into two steps
    std::vector<int> two_digit;
    std::ranges::transform(in1, in2,
        std::back_inserter(two_digit),
        std::plus<>{},                // reduction
        [](int v) { return v * 10; }, // in1 projection
        std::identity{});             // in2 projection
    // two_digit == {10, 21, 30, 41, 50}
    
    std::println("two_digit == {}", two_digit);
}
```
The C++23 std::views::zip produces a view of tuple-like elements, each consisting of the corresponding elements from all the adapted views.

The shortest range determines the number of elements in the view.

The elements of the view maintain reference semantics, meaning that if the arguments are mutable ranges, their elements can be mutated through the elements of this view.
https://compiler-explorer.com/z/rEWh65hb7

```c++

int main() {
    std::vector<int> first{1,2,3,4,5};
    std::vector<double> second{9,8,7,6};

    // Iterate over the elements of the zip view
    for (auto [a, b] : std::views::zip(first, second)) {
        // {1,9}, {2,8}, {3,7}, {4,6}
        std::cout << a << " " << b << "\n";
    }
    std::cout << "\n";

    // Same as above, without structured binding
    for (std::tuple<int&,double&> el : std::views::zip(first, second)) {
        // {1,9}, {2,8}, {3,7}, {4,6}
        std::cout << std::get<0>(el) << " " << std::get<1>(el) << "\n";
    }
    std::cout << "\n";
    
    std::vector<std::string> third{"label1", "label2", "label3"};

    // The zip view can accept one or more arguments
    for (auto [a, b, c] : std::views::zip(first, second, third)) {
        // {1,9,"label1"}, {2,8,"label2"}, {3,7,"label3"}
        std::cout << a << " " << b << " " << std::quoted(c) << "\n";
    }
    std::cout << "\n";

    // We can also modify the original ranges through the tuple
    for (auto [a, b, c] : std::views::zip(first, second, third)) {
        a = a + b;
        std::cout << a << " " << b << " " << std::quoted(c) << "\n";
    }
    std::cout << "\n";
    // first == {10, 10, 10, 4, 5}

    for (auto v : first)
        std::cout << v << " ";
    std::cout << "\n";
}
```
Dependency injection is a simple design pattern that decouples a component from its dependency.

While the pattern introduces a virtual dispatch (with its runtime cost), its simplicity and effect on testability are usually worth it.
https://compiler-explorer.com/z/1976811fd

```c++

struct Service { // Base interface
    virtual void make_important_call() = 0;
};

struct FakeService : Service { // Fake implementation for testing
    void make_important_call() override {
        std::cout << "Fake Service.\n";
    }
};

struct ProductionService : Service { // Real implementation
    void make_important_call() override {
        std::cout << "Production Service.\n";
    }
};

struct MyClass {
    MyClass(std::unique_ptr<Service> service) : // Inject dependency
        dependency_(std::move(service)) {}

    void operate() {
        dependency_->make_important_call();
    }
private:
    std::unique_ptr<Service> dependency_;
};

int main() {
    // In production code:
    MyClass m(std::make_unique<ProductionService>());
    m.operate();

    // In test code:
    MyClass n(std::make_unique<FakeService>());
    n.operate();
}
```
One option to distinguish two function calls that would otherwise fully overlap in their argument types is using type tagging.

This technique is beneficial for constructors since constructors cannot be renamed.
https://compiler-explorer.com/z/Eb4eTET3v

```c++

struct IPv4_t{};
struct IPv6_t{};

constexpr inline IPv4_t IPv4 = {};
constexpr inline IPv6_t IPv6 = {};

struct IPAddr {
    IPAddr(IPv4_t, std::string_view addr, int port) {}
    IPAddr(IPv6_t, std::string_view addr, int port) {}
};

int main() {
    IPAddr addr1(IPv4, "127.0.0.1", 80);
    IPAddr addr2(IPv6, "::1", 80);
}
```
The std::lower_bound and std::upper_bound are arguably the two most practically useful algorithms in the standard library.

Both algorithms are binary searches, operating in O(logn) on sorted ranges.

The std::lower_bound returns an iterator to the first element not ordered before the provided value and std::upper_bound to the first element ordered after the provided value.
https://compiler-explorer.com/z/nhE6oKear

```c++

int main() {
    std::vector<int> data{1,2,3,4,5,5,5,6,7,8,9};

    // First element for which el < value == false
    auto lb = std::lower_bound(data.begin(), data.end(), 5);
    // *lb == 5

    std::println("*lb == {}", *lb);
    
    // same as:
    lb = std::partition_point(data.begin(), data.end(), [](int el) {
        return el < 5;
    });
    // *lb == 5

    std::println("*lb == {}", *lb);

    // First element for which value < el == true
    auto ub = std::ranges::upper_bound(data, 5); // range version
    // *ub == 6

    std::println("*ub == {}", *ub);

    // same as:
    ub = std::ranges::partition_point(data, [](int el) {
        return not (5 < el);
    });
    // *ub == 6

    std::println("*ub == {}", *ub);

    // [begin, lb) forms the subrange of elements lower than value
    auto lower = std::ranges::subrange(data.begin(), lb);
    // lower == {1, 2, 3, 4}

    std::println("lower == {}", lower);

    // [lb, ub) forms the subrange of elements equal to the value
    auto equal = std::ranges::subrange(lb, ub);
    // equal == {5, 5, 5}

    std::println("equal == {}", equal);

    // [ub, end) forms the subrange of elements higher than the value
    auto high = std::ranges::subrange(ub, data.end());
    // high == {6, 7, 8, 9}

    std::println("high == {}", high);

    std::vector<std::string> strs{"a","ab","fge","cdefgh"};

    // Find the first string that is at least 2 characters long
    auto el = std::ranges::lower_bound(strs, 2,
        std::less<>{},              // Custom comparator
        [](const std::string& el) { // Projection
            return el.length();
        });
    // *el == "ab"

    std::println("*el == {}", *el);
}
```
The C++23 introduced a set of range-enabled fold algorithms to replace the std::accumulate numeric algorithm.

The library provides both left and right folds: std::ranges::fold_left and std::ranges::fold_right.

As well as variants that use the first/last elements as initializers: std::ranges::fold_left_first and std::ranges::fold_right_last.
https://compiler-explorer.com/z/bKf566q53

```c++

int main() {
    std::vector<int64_t> data{1,2,3,4,5,6};

    // Left fold, with initial value.  
    auto v = std::ranges::fold_left(data, 10, std::plus<>{});
    // Unlike with std::accumulate, the result type is based
    // on the invocation result of operation(init, elem).
    // decltype(v) == int64_t, v == 31

    static_assert(std::is_same_v<decltype(v),int64_t>);
    std::cout << "v == " << v << "\n";

    // Right fold, using the last element as initializer.
    auto w = std::ranges::fold_right_last(data, std::plus<>{});
    // The result is a std::optional to accomodate empty ranges.
    // decltype(w) == std::optional<int64_t>, w == 21

    static_assert(std::is_same_v<decltype(w),std::optional<int64_t>>);
    std::cout << "w == " << *w << "\n";

    std::vector<int64_t> empty;
    // Left fold, using the first element as initializer.
    auto z = std::ranges::fold_left_first(empty, std::plus<>{});
    // z.has_value() == false
    std::cout << "z.has_value() == " << std::boolalpha << z.has_value() << "\n";
}
```
Init statements enable fully encapsulated if, switch (C++17) and range-for loop (C++20) statements by adding an additional init section that can be used to create variables with a lifetime matching the statement block.
https://compiler-explorer.com/z/ndn1n9h4e

```c++

std::optional<int> some_call() {
    return 10;
}

int other_call() { return 10; }

int main() {
    // C++17
    if (auto result = some_call(); result.has_value()) {
        int v = *result;
    }

    // C++17
    switch (auto val = other_call(); val) {
        case 1: // further process val
            break;
        case 2: // further process val
            break;
        default:
            ;
    }

    // C++20 / Enables iteration over temporary ranges.
    for(std::string v = std::to_string(42); auto c : v) {
        // v is a valid range that can be safely iterated over
    }
}
```
std::views::adjacent is a view similar to std::views::slide, producing a sliding window over the input range. However, where std::views::slide produces subranges, std::views::adjacent produces tuples of references to elements.

Consequently, the elements of std::views::adjacent can be deconstructed using structured binding.
https://compiler-explorer.com/z/xq8oPvaxa

```c++

int main() {
    std::vector<int> data{1,2,3,4,5};

    // "sliding tuple" of references to elements
    for (std::tuple<int&,int&,int&> v : data | std::views::adjacent<3>) {
        std::cout << std::get<0>(v) << " " << std::get<1>(v) << " " << std::get<2>(v) << "\n";
    }
    
    std::cout << "\n";

    // deconstructed using structured binding
    for (auto [first, second, third] : data | std::views::adjacent<3>) {
        std::cout << first << " " << second << " " << third << "\n";
    }

    std::cout << "\n";

    // std::views::adjacent<2> has an alias
    for (auto [left, right] : data | std::views::pairwise) {
        std::cout << left << " " << right << "\n";
    }
}
```
C++20 introduced the [[no_unique_address]] attribute that can be used to annotate non-static data members.

The attribute allows the compiler to optimize empty members marked with this attribute to take up no space.
https://compiler-explorer.com/z/4Y9dK95jf

```c++

struct Empty {};

struct Bigger {
    int value;
    // Even though empty, has to take up at least one byte.
    Empty empty;
};

struct SameAsInt {
    int value;
    // Allowed to overlap.
    [[no_unique_address]] Empty empty;
};

int main() {
    static_assert(sizeof(Empty) >= 1);
    static_assert(sizeof(Bigger) >= sizeof(int) + 1);
    static_assert(sizeof(SameAsInt) == sizeof(int));
}
```
The std::endian from C++20 is an enum in the &lt;bit&gt; header that provides information about the native endianness of the architecture the program was compiled for.
https://compiler-explorer.com/z/6EW5G1a3f

```c++

int main() {
    if constexpr (std::endian::native == std::endian::little) {
        std::cout << "This system is little-endian.\n";
    } else if constexpr (std::endian::native == std::endian::big) {
        std::cout << "This system is big-endian.\n";
    }
}
```
The C++20 std::views::keys and std::views::values are specialized views that represent the views over the keys and values of associate containers.

Reminder: for std::set and std::unordered_set (and their multi-variants), the elements are keys, not values.
https://compiler-explorer.com/z/z7Pqq8WjT

```c++

int main() {
    std::unordered_map<int,int> data{{1,7}, {2,8}, {3,9}};

    std::cout << "keys   : ";
    for (const auto &v : data | std::views::keys) {
        std::cout << v << " ";
    }
    std::cout << "\n";

    std::cout << "values : ";
    for (const auto &v : data | std::views::values) {
        std::cout << v << " ";
    }
    std::cout << "\n";
}
```
If you want to look up an element in a range by value or by predicate, the most straightforward options are the three linear find algorithms:

• std::find (find element by value)<br />• std::find_if (find element using a positive predicate)<br />• std::find_if_not (find element using a negative predicate)

All three algorithms have parallel (C++17) and range variants (C++20).
https://compiler-explorer.com/z/1j51xf7fP

```c++

int main() {
    std::vector<int> data{1, 2, 3, 4, 5, 6, 7};

    // Find by value
    auto it = std::find(data.begin(), data.end(), 5);
    // *it == 5

    std::println("*it == {}", *it);

    it = std::find(data.begin(), data.end(), 42);  
    // it == data.end()

    std::println("(it == data.end()) == {}", it == data.end());


    // Find using a predicate
    auto is_even = [](int v) { return v % 2 == 0; };
    it = std::find_if(data.begin(), data.end(), is_even);
    // *it == 2

    std::println("*it == {}", *it);

    // Find using a negation of a predicate
    it = std::find_if_not(data.begin(), data.end(), is_even);
    // *it == 1

    std::println("*it == {}", *it);

    // same as
    it = std::find_if(data.begin(), data.end(), std::not_fn(is_even));
    // *it == 1

    std::println("*it == {}", *it);

    // Range version
    it = std::ranges::find(data, 3);
    // *it == 3

    std::println("*it == {}", *it);

    // With a projection
    std::vector<std::string> strs{"a", "bb", "ccc", "dddd"};
    // Find the first element that has an even length
    auto it2 = std::ranges::find_if(strs, is_even, 
        [](const std::string& str) {
            return str.length();
        });
    // *it2 == "bb"

    std::println("*it2 == {}", *it2);
}
```
The range-for-loop is a very convenient replacement for the C-style for-loop.

However, range-for-loop can be cumbersome when accessing the original index or computing a target index.

The C++23 std::views::enumerate removes this problem by producing a range of tuples, where the i-th tuple consists of the value i and a reference to the i-th element of the source range.
https://compiler-explorer.com/z/84b6ns5hs

```c++

int main() {
    std::vector<int> data{1, 2, 3, 4, 5, 6, 7};
    
    // Applying enumerate before other views to inject original indexes
    constexpr auto before = std::views::enumerate | std::views::filter([](auto t) {
        return std::get<1>(t) % 2 == 0;
    });
    for (auto [idx, value] : data | before) {
        // Iterate over: {1,2}, {3,4}, {5,6}
        std::cout << idx << " -> " << value << "\n";
    }
    std::cout << "\n";

    // Applying enumerate after other views to index the resuling range
    constexpr auto after = std::views::filter([](int v) { 
        return v % 2 == 0;
    }) | std::views::enumerate;
    for (auto [idx, value] : data | after) {
        // Iterate over: {0,2}, {1,4}, {2,6}
        std::cout << idx << " -> " << value << "\n";
    }
    std::cout << "\n";

    // Since the second element of the tuple is a reference,
    // we maintain mutability
    for (auto [idx, value] : data | std::views::enumerate) {
        value -= idx;
    }
    // data == {1,1,1,1,1,1,1}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";
}
```
The std::ranges::fold_left_with_iter and std::ranges::fold_left_first_with_iter are alternative versions of the fold_left algorithms that additionally return the computed end iterator.

This comes in handy in cases where the range is terminated using a sentinel. The computed end iterator can then be re-used and doesn&#39;t have to be re-calculated.
https://compiler-explorer.com/z/rsxK669fe

```c++

// Utility to get the absolute base of a nested iterator type
template <typename T> auto absolute_base(T t) { 
    if constexpr (requires (T t) {
        { t.base() } -> std::same_as<typename T::iterator_type>;
    }) return absolute_base(t.base()); else return t;
}


int main() {
    std::vector<int> data{1,2,3,4,5,6,7,8,9};

    // a view without the last 3 elements
    constexpr auto view = std::views::reverse | std::views::drop(3) | std::views::reverse;
    auto [it, value] = std::ranges::fold_left_with_iter(data | view, 0, std::plus<>{});
    // value == 21 (0+1+2+3+4+5+6)
    std::cout << value << "\n";
    
    // "it" is a reverse iterator of a reverse iterator, therefore we can't do:
    // it == data.end(), but we can do absolute_base(it) == data.end()
    for (auto v : std::ranges::subrange(absolute_base(it), data.end())) {
        // process the rest of the range
        // iterate over {7, 8, 9}
        std::cout << v << " ";
    }
}
```
Singletons should be avoided if possible, as they introduce a hurdle for testing. However, sometimes a global state might be the least bad choice.

If you need a Singleton, use a simple getter function that returns a reference to a local static variable.

Block-scope static variables are initialized in a thread-safe and exception-safe manner.
https://compiler-explorer.com/z/3MdMsTedj

```c++

struct GlobalData {
    int state;
private:
    // Optional: prevent GlobalData to be constructible
    // outside of get_global_data()
    GlobalData(int state) : state(state) {}
    friend GlobalData& get_global_data();
};

// a.k.a. Scott Meyer's singleton
GlobalData& get_global_data() {
    // Static block-scope variables have static storage duration
    // and are initialized on the first time the control passes
    // through the declaration in a thread-safe manner.
    static GlobalData storage{42};
    return storage;

    // Note: if the initialization throws, it will be re-attempted 
    // on the next pass-through.
}


int main() {
    std::cout << "state == " << get_global_data().state << "\n";
}
```
The C++20 std::views::transform can be used to lazily produce a range of transformed elements, including the possibility of changing the range&#39;s element type.
https://compiler-explorer.com/z/vee5Y9YTK

```c++

int main() {
    std::vector<int> data{1,2,3,4,5};

    // produce a view of squared values
    constexpr auto as_squared = std::views::transform([](int v) { return v * v; });
    for (int v : data | as_squared) {
        // iterate over: 1,4,9,16,25
        std::cout << v << " ";
    }
    std::cout << "\n";

    // views::transform can produce a view with a different element type
    constexpr auto as_strings = std::views::transform([](int v) { return std::to_string(v); });
    for (std::string v : data | as_strings) {
        // iterate over: "1"s,"2"s,"3"s,"4"s,"5"s
        std::cout << v << " ";
    }
    std::cout << "\n";
}
```
Support for projections is one of the improvements introduced with the C++20 range versions of standard algorithms.

Projections are applied before elements are passed to the corresponding invocable.

Algorithms that operate on multiple source ranges provide a separate projection for each range.

Note that taking an address of standard functions (including members) is UB.
https://compiler-explorer.com/z/W1GYv1aET

```c++

struct User {
    int64_t id;
    std::string name;
};

int main() {
    using namespace std::literals;
    std::vector<User> users{{37,"Eliana Green"}, {23, "Logan Sterling"}, {1, "Isla Bennett"}, {7, "Marcel Jones"}};

    // Sort users by id (any invocable will work)
    std::ranges::sort(users, {}, &User::id);
    // {} instantiates the default comparator: std::ranges::less
    // std::ranges::sort(users, std::ranges::less{}, &User::id);

    // users == {{1,"Isla..."}, {7,"Marcel..."}, {23,"Logan..."}, {37,"Eliana..."}}

    for (auto v : users)
        std::cout << v.id << ": " << v.name << "\n";

    auto it = std::ranges::find(users, "Eliana Green"s, &User::name);
    // it->id == 37, it->name == "Eliana Green"
    std::cout << it->id << ": " << it->name << "\n";

    std::vector<int> first{1,2,3,4,5};
    std::vector<int> second{1,2,3,4,5};
    std::vector<int> out;

    std::ranges::transform(first, second, std::back_inserter(out), 
        [](int left, int right) { return left * right; }, // transformation operation
        [](int left) { return left + 10; },   // projection for first range
        [](int right) { return right / 2; }); // projection for second range
    // out == 0 (11*0), 12 (12*1), 13 (13*1), 28 (14*2), 30 (15*2)

    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";
}
```
std::function is a storage type that can store callable entities (functions, function objects, lambdas) that conform to the specified call signature.

The container is copyable (which it also requires of the wrapped object) and will throw when invoked without a stored state.
https://compiler-explorer.com/z/d4a55K8oT

```c++

double sum(const std::vector<double>& data) {
    return std::accumulate(data.begin(), data.end(), 0.);
}

int main() {
    std::vector<double> data = {0.5, 1.0, 2.3};

    std::function<double(const std::vector<double>&)> fn;
    try {
        fn(data); // Throws std::bad_function_call because empty.
    } catch (const std::bad_function_call& e) {
        std::println("e.what() == {}", e.what());
    }

    // Store a function in fn
    fn = sum;
    auto res1 = fn(data);
    // res1 == 3.8

    std::println("res1 == {}", res1);

    // Store a capturing lambda in fn
    int offset = 3;
    fn = [offset](const std::vector<double>& v) {
        return std::ranges::max(v) + offset;
    };
    auto res2 = fn(data);
    // res2 == 5.3

    std::println("res2 == {}", res2);

    // Despite following value semantics, 
    // std::function doesn't respect const
    struct Fun { int x = 0; void operator()() { ++x; } };

    const Fun fun;
    // fun(); // will not compile, would discard const

    const std::function<void()> mutates = Fun{};
    mutates(); // OK
    // mutates.target<Fun>()->x == 1

    std::println("mutates.target<Fun>()->x == {}", mutates.target<Fun>()->x);

    /* The stored object has to be copyable
    Wouldn't compile, captured std::unique_ptr makes the lambda move-only
    fn = [ptr = std::make_unique<int>(10)](const std::vector<double>& v) {
        return std::ranges::max(v) + *ptr;
    };
    */
}
```
The C++23 std::views::zip_transform is a view where the ith element results from applying the n-ary transformation invocable to the ith elements of the provided ranges.

The view is a lazy version of binary std::ranges::transform, generalized for any number of ranges.
https://compiler-explorer.com/z/73hYdTMvW

```c++

int main() {
    std::vector<int> first{1,2,3,4};
    std::vector<double> second{9.1, 9.2, 9.3, 9.4, 9.5};
    std::vector<std::string> third{"a", "b", "c", "d", "e"};

    // transform function takes one element from every range
    auto zip = [](int& a, double& b, std::string& c) {
        return a+b+(c[0]-'a');
    };
    // produce a view where the i-th element is the result of applying
    // the transformation function the i-th elements of the provided ranges
    // the length is determined by the shortest of the ranges
    for (auto v : std::views::zip_transform(zip, first,second,third)) {
        // iterate over: 10.1, 12.2, 14.3, 16.4
        std::cout << v << " ";
    }
    std::cout << "\n";
}
```
The C++23 std::move_only_function is a move-only version of std::function.

Apart from allowing for non-copyable function objects, the std::move_only_function fully supports invocable semantics and respects const.
https://compiler-explorer.com/z/o5rKf6cbo

```c++

struct X {
    void operation(int) {};
    int x;
};

int main() {
    // Will not compile, lambda not copy constructible
    // because anchor is not copy constructible.
    // std::function<void(void)> a = [anchor=std::make_unique<int>(42)](){};

    // OK
    std::move_only_function<void(void)> b = [anchor=std::make_unique<int>(42)](){};


    // invocable support (note that the type is as-if it was a regular function)
    // lookup "Abominable Function Types" for additional context
    std::move_only_function<void (X&, int)> c = &X::operation;
    std::move_only_function<int (X&)> d = &X::x;

    X x;
    c(x, 42); // invokes x.operation(42);
    int y = d(x); // same as int y = x.x;

    // respects const
    const std::move_only_function<void(void)> e = [](){};
    // e(); // will not compile, can store, but not invoke a non-const functions
    const std::move_only_function<void(void) const> f = [](){};
    f(); // OK
}
```
Raw string literals (introduced in C++11) simplify the inclusion of multi-line text and strings that contain special characters that would otherwise have to be escaped.
https://compiler-explorer.com/z/acEeoGrTs

```c++

int main() {
// R prefix followed by "( closed by )"
std::string_view a = R"(c:\some\file\path)";
// c:\some\file\path

std::cout << std::quoted(a) << "\n";

// Optionally, an identifier can be included, to allow for a )" substring
std::string_view b = R"some_data(R"(this is a raw string)")some_data";
// R"(this is a raw string)"

std::cout << std::quoted(b) << "\n";

std::string_view c = R"(When
using multi-line raw strings
    any leading space and newline characters
  will be part of the string)";

std::cout << std::quoted(c) << "\n";
}
```
The trio of std::ranges::find_last, std::ranges::find_last_if and std::ranges::find_last_if_not are C++23 algorithms that find the last element matching the provided value or predicate.

While we could use std::ranges::find on bidirectional ranges with the same effect, these variants can operate on forward ranges.
https://compiler-explorer.com/z/Mfz7Wj9z6

```c++

int main() {
    std::forward_list<int> data{1,2,3,4,5,6,7};
    {
    auto [it, end] = std::ranges::find_last(data, 5);
    // *it == 5, end == data.end()
    std::cout << "*it == " << *it << "\n";
    }

    auto is_even = [](int v) { return v % 2 == 0; };
    {
    auto [it, end] = std::ranges::find_last_if(data, is_even);
    // *it == 6, end == data.end()
    std::cout << "*it == " << *it << "\n";
    }

    // the calculated end is useful when working with lazy ranges
    {
    auto counted = std::views::counted(data.begin(), 5);
    auto [it, end] = std::ranges::find_last_if(counted, is_even);
    // *it == 4, *end == 6
    std::cout << "*it == " << *it << "\n";
    std::cout << "*end == " << *end << "\n";    
    }
}

```
The C++11 std::this_thread is a namespace in the &lt;thread&gt; header that contains functions for querying the current thread id and yielding the current thread execution.

Note that the actual behaviour of yield, sleep_for and sleep_until very much depends on the thread library and OS scheduler.
https://compiler-explorer.com/z/hzcb6eKde

```c++

int main() {
    // Print the current thread id
    std::cout << std::this_thread::get_id() << "\n";  

    // Yield execution of this thread, allowing other threads to run
    std::this_thread::yield();

    using namespace std::chrono;
    // Yield execution of this thread for at least the specified duration
    std::this_thread::sleep_for(240ms);
    // Yield execution of this thread until at least the specified time point
    std::this_thread::sleep_until(steady_clock::now() + 1s);
}

```
C++17 constexpr if replaces most cases where macros would have to be used.

It also simplifies generic functions and metaprogramming and works with C++20 concepts to allow code to adjust based on features supported by the type of the function arguments.
https://compiler-explorer.com/z/vT99WMoTr

```c++

constexpr inline bool feature_x24_enabled = true;

// Macro replacement
void some_function() {
    if constexpr (feature_x24_enabled) {
        // something special for feature x24
    }
}

// Function returning different types based on type of argument
auto generic_function(auto x) {
    if constexpr (std::is_integral_v<decltype(x)>) {
        // argument integral -> returns std::string
        return std::to_string(x);
    } else if constexpr (std::is_floating_point_v<decltype(x)>) {
        // argument floting point type -> returns int64_t
        return static_cast<int64_t>(x);
    } else {
        // otherwise return void
        return;
    }
}

// Works with C++20 concepts:
void custom_algorithm(auto&& rng) {
    using type = decltype(rng);
    if constexpr (std::ranges::random_access_range<type>) {
        // implementation for random_access_range
    } else if constexpr (std::ranges::bidirectional_range<type>) {
        // implementation for bidirectional_range
    } else {
        // fallback implementation
    }
}

int main() {
    auto r1 = generic_function(10);
    // decltype(r1) == std::string
    static_assert(std::is_same_v<decltype(r1),std::string>);

    auto r2 = generic_function(2.4);
    // decltype(r2) == int64_t
    static_assert(std::is_same_v<decltype(r2),int64_t>);
}

```
The C++23 std::views::join_with will, similar to std::views::join, flatten a range of ranges but will additionally insert the provided element in between every pair of sub-ranges.
https://compiler-explorer.com/z/7GWo7vx1Y

```c++

int main() {
    std::vector<std::vector<int>> data = {{1,2,3}, {4,5,6}, {7,8,9}};

    // A flattened view of data with 0 inserted in between every subrange
    auto flattened = data | std::views::join_with(0) | std::views::common;
    
    std::vector<int> flat(flattened.begin(), flattened.end());
    // flat == {1,2,3,0,4,5,6,0,7,8,9}

    for (auto v : flat) {
        std::cout << v << " ";
    }
    std::cout << "\n";


    std::string greeting = "Hello World!";
    
    // Split by space and re-join using newlines
    auto lined = greeting | std::views::lazy_split(' ') | 
        std::views::join_with('\n') | std::views::common;
    
    std::string new_greeting(lined.begin(), lined.end());
    // new_greeting == "Hello\nWorld!";

    std::cout << new_greeting << "\n";
}
```
One downside of standard views is their behaviour when marked as const.

As is typical with reference types, views do not respect const. Unfortunately, on top of that, certain combinations of views and ranges require mutability.

With the C++23 std::views::as_const, we can leave the views mutable while enforcing the immutability of the underlying data.
https://compiler-explorer.com/z/rqjv7Wz85

```c++

int main() {
    std::vector<int> rnd_acc{1,2,3,4,5,6};
    std::list<int> bidir{1,2,3,4,5,6};
    
    const auto v1 = rnd_acc | std::views::drop(1);
    auto x1 = *v1.begin(); // OK
    *v1.begin() = 42; // Also OK (not respecting const)

    const auto v2 = bidir | std::views::drop(1);
    // auto x2 = *v2.begin();
    // Wouldn't compile, views::drop requires mutability to operate on
    // non-random-access ranges.

    auto cview1 = rnd_acc | std::views::as_const | std::views::drop(1);
    auto cview2 = bidir | std::views::as_const | std::views::drop(1);

    auto y1 = *cview1.begin(); // OK
    auto y2 = *cview2.begin(); // OK

    // *cview1.begin() = 42;
    // Wouldn't compile, can't mutate through const reference.
}
```
The C++11 std::async is a simple tool for launching asynchronous tasks to either start a task in parallel or to defer execution on the same thread.

The benefit of std::async is simplicity, as std::async directly returns a std::future; however, we pay for that simplicity with a lack of control.
https://compiler-explorer.com/z/x3Kaq9eed

```c++

int main() {
    using namespace std::literals;

    // main thread
    std::osyncstream(std::cout) << "Main thread: " << std::this_thread::get_id() << "\n";
    auto t1 = std::chrono::steady_clock::now();

    // Spawn a task to run asynchronously (in a thread)
    auto result = std::async(std::launch::async,[](){
        std::osyncstream(std::cout) << "Running in a thread: " << std::this_thread::get_id() << "\n";
        std::this_thread::sleep_for(200ms);
        std::osyncstream(std::cout) << "Finished thread: " << std::this_thread::get_id() << "\n";
    });

    auto t2 = std::chrono::steady_clock::now();
    std::osyncstream(std::cout) << "Launching took: " << (t2-t1) << "\n";

    // Wait for the task to finish
    result.wait();

    auto t3 = std::chrono::steady_clock::now();
    std::cout << "Asynchronous call now finished: " << (t3-t1) << "\n";

    // std::async returns a std::future
    auto future = std::async(std::launch::async, [](){
        // Slow operation
        return 42;
    });

    // Block until the result is available and then retrieve it
    auto value = future.get();
    // value == 42

    std::cout << "value == " << value << "\n";

    // We can use async, to defer execution of code until the end of the scope
    {
    FILE *file = fopen("/dev/null", "r");
    auto deferred_close = std::async(std::launch::deferred, [file](){
        fclose(file);
    });
    } // destructor for deferred_close runs and closes the file
}
```
Correction for previous <a href="https://hachyderm.io/tags/dailybiteofcpp" class="mention hashtag" rel="tag">#<span>dailybiteofcpp</span></a>. <br />Unfortunately, I have misinterpreted a corner case behaviour of std::async, and it managed to survive in the example.

The packaged task will not be run automatically when the future is destroyed. You still need to call .wait() or .get() manually.

Thanks to Pavel Pokutnev on LinkedIn for pointing it out.
https://compiler-explorer.com/z/4nPjMxxfj

```c++

int main() {
    bool did_run = false;
    {
    FILE *f = fopen("/dev/null", "r");
    auto def = std::async(std::launch::deferred, 
        [f,&did_run](){
            fclose(f);
            did_run = true;
        });

    // other code

    // comment this out to see did_run == false
    def.wait();
    }
    // did_run == true
    std::cout << std::boolalpha << did_run << "\n";
}
```
std::quoted is a C++14 utility that simplifies reading and writing quoted strings.

When used during stream insertion, it will surround the given string with quotes and escape any internal quotes.

When used during stream extraction, the extraction will read the entire quoted string, remove surrounding quotes (if any) and unescape internal quotes.
https://compiler-explorer.com/z/9s5318b9j

```c++

int main() {
std::cout << std::quoted(R"(I say: "Hello Wordl!")") << "\n";
// prints: "I say: \"Hello World!\""

std::stringstream s(R"("I say: \"Hello World!\"")");
std::string unescaped;
s >> std::quoted(unescaped);
// unescaped == I say: "Hello World!"
std::cout << unescaped << "\n";

// Escape and quote characters can be customized.
std::cout << std::quoted("10|20|30",'|','#') << "\n";
// prints: |10#|20#|30|
}
```
Integer literals can be specified in decimal, octal, hexadecimal and since C++14 binary bases.

Further, since C++14, the single-quote character can be used as a digit separator without changing the value.
https://compiler-explorer.com/z/qTqh384E6

```c++

int main() {
// decimal: non-zero decimal digit (1, 2, 3, 4, 5, 6, 7, 8, 9), 
// followed by zero or more decimal digits (0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

int i = 42;

std::cout << "i == " << i << "\n";

// octal: digit zero (0) 
// followed by zero or more octal digits (0, 1, 2, 3, 4, 5, 6, 7)

int j = 072; // 58 decimal

std::cout << "j == " << j << "\n";

// hexadecimal: character sequence 0x or the character sequence 0X 
// followed by one or more hexadecimal digits (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 
// a, A, b, B, c, C, d, D, e, E, f, F)

int k = 0XFF; // 255 decimal

std::cout << "k == " << k << "\n";

// binary: character sequence 0b or the character sequence 0B 
// followed by one or more binary digits (0, 1)
// C++14

int l = 0B1101; // 13 decimal

std::cout << "l == " << l << "\n";

// Digits can be separated using ' for improved readability.
// C++14

int m = 1'000'000;

std::cout << "m == " << m << "\n";
}
```
std::next and std::prev are C++11 iterator utilities that return the succeeding or preceding iterator.

If the provided iterator models random access, the operation will be constant, even if a custom distance is specified.
https://compiler-explorer.com/z/bKa9aff3h

```c++

int main() {
std::vector<int> data{1, 2, 3, 4, 5, 6, 7};

// std::prev requires bidirectional iterator
auto it1 = std::prev(data.end());
// *it1 == 7

std::cout << "*it1 == " << *it1 << "\n";

// distance can be customized
auto it2 = std::next(data.begin(), 3);
// *it2 == 4

std::cout << "*it2 == " << *it2 << "\n";

std::list<int> lst{1, 2, 3, 4, 5, 6, 7};

// for non-random-access iterators the operation is linear
auto it3 = std::prev(lst.end(), 4);
// *it3 == 4

std::cout << "*it3 == " << *it3 << "\n";
}
```
Structured bindings were introduced in C++17.

They allow the decomposition of arrays, tuples and non-static data members into named identifiers.

The original object is captured following the standard deduction rules for auto; the identifiers are effectively transparent references into the captured object.
https://compiler-explorer.com/z/4TKWW3n6M

```c++

struct UserType {
    int a;
    double b;
    std::string c;
};

int main() {
    // typical use case for structured binding to decompose 
    // a pair returned by some standard library operations
    std::unordered_set<int> data{1, 2, 3, 4, 5};
    if (auto [it, inserted] = data.insert(4); inserted) {
        // insertion happened
        // it points to the newly inserted element  
    } else {
        // no insertion happened
        // it points to the existing element
    }

    // Normal auto deduction rules still apply
    std::pair a{10,20}; // mutable
    const std::pair b{10,20}; // immutable

    auto [i,j] = a; // Capture by copy
    i = 0; j = 0; // OK
    // a.first == 10, a.second == 20

    std::cout << "i == " << i << ", j == " << j << "\n";
    std::cout << "a.first == " << a.first << ", a.second == " << a.second << "\n\n";

    auto &[k,l] = a; // Capture by reference
    k = 0; l = 0; // OK
    // a.first == 0, a.second == 0

    std::cout << "k == " << k << ", l == " << l << "\n";
    std::cout << "a.first == " << a.first << ", a.second == " << a.second << "\n\n";

    auto &[m,n] = b; // Capture by reference, auto will deduce const
    // m = 0; j = 0; // Not OK, cannot mutate read-only variable

    std::cout << "m == " << m << ", n == " << n << "\n\n";

    UserType u{42, 3.14, "Hello World!"};
    // Decomposing an aggregate
    auto &[x,y,z] = u;
    // x == 42, y == 3.14, z == "Hello World!"

    std::cout << "x == " << x << ", y == " << y << ", z == " << std::quoted(z) << "\n";
}
```
Before C++20, a range was an implicit concept represented by two iterators. With C++20, this concept was formalized and relaxed to an iterator and a sentinel.

To adapt a range for old code that requires a common range (iterator and sentinel of the same type), we can use the std::views::common adapter view.
https://compiler-explorer.com/z/zPvMsrc1E

```c++

int main() {
    // std::views::iota(1) is not a common view
    auto view = std::views::iota(1) | std::views::take(3);
    
    // Will not compile, view.begin() and view.end() are of different types
    // int sum = std::accumulate(view.begin(), view.end(), 0);

    auto common_view = view | std::views::common;
    int sum = std::accumulate(common_view.begin(), common_view.end(), 0); // OK
    // sum == 6

    std::cout << "sum == " << sum << "\n";
}
```
String literals in C++ are immutable character arrays (for compatibility with C).

This can be inconvenient when working with generic functions or when using auto.

Since C++14, the standard library provides support for standard string literals, that is, literals of type std::string and its variants.
https://compiler-explorer.com/z/ocn3szTKq

```c++

int main() {
    using namespace std::string_literals;

    auto s1 = "Hello World!"s;
    // decltype(s1) == std::string

    static_assert(std::is_same_v<decltype(s1), std::string>);

    auto s2 = L"😀"s;
    // decltype(s2) == std::wstring

    static_assert(std::is_same_v<decltype(s2), std::wstring>);

    // utf8 string literal since C++20
    auto s3 = u8"🙃"s; 
    // decltype(s3) == std::u8string

    static_assert(std::is_same_v<decltype(s3), std::u8string>);

    auto s4 = u"😜"s;
    // decltype(s4) == std::u16string

    static_assert(std::is_same_v<decltype(s4), std::u16string>);

    auto s5 = U"🤔"s;
    // decltype(s5) == std::u32string

    static_assert(std::is_same_v<decltype(s5), std::u32string>);
}
```
A default-constructed std::variant will always initialize with its first element as active.

This can be problematic because it requires the value construction of the first element (can be unsupported or costly).

To avoid this issue and allow a semantically &quot;empty&quot; std::variant, we can use the special type std::monostate as the first argument of the std::variant.
https://compiler-explorer.com/z/sWjPEadv1

```c++

struct X {
    X(int v) : v_(v) {}
    int v_;
};

struct Y {
    Y(double v) : v_(v) {}
    double v_;
};

int main() {
    // X, Y are non-default constructible
    // std::variant<X,Y> a; wouldn't not compile, can't default construct

    std::variant<X,Y> a = Y{20}; // OK
    // a.index() == 1

    std::println("a.index() == {}", a.index());

    std::variant<std::monostate,X,Y> b; // OK, semantically "empty"
    // b.index() == 0, std::holds_alternative<std::monostate>(b) == true

    std::println("b.index() == {}", b.index());
    std::println("std::holds_alternative<std::monostate>(b) == {}", std::holds_alternative<std::monostate>(b));

    std::variant<int,double> c;
    std::variant<int,double> d{0};
    // c == d

    // to distinguish an empty state:
    std::variant<std::monostate,int,double> e;
    std::variant<std::monostate,int,double> f{0};
    // e != f

    std::println("(c == d) == {}", c == d);
    std::println("(e == f) == {}", e == f);
}
```
Anonymous (unnamed) namespaces are a great C++11 tool for controlling the linkage of symbols.

Any symbols inside an unnamed namespace will have internal linkage, i.e. the symbols are only visible to the translation unit.
https://compiler-explorer.com/z/fz9b95nPs

```c++
cmake_minimum_required(VERSION 3.12)

project(main)
add_library(unit unit.cc unit.h)
add_executable(main main.cc)
target_link_libraries(main unit)
```
The C++20 std::views::drop and std::views::drop_while are views that omit leading elements of the underlying range.

std::views::drop will omit the specified number of leading elements. std::views::drop_while will omit the leading elements that satisfy the provided predicate.
https://compiler-explorer.com/z/ozqhKcM7j

```c++

int main() {
    std::vector<int> data{1,2,3,4,5};

    // iterate over {2,3,4,5}
    for (auto v : data | std::views::drop(1))
        std::cout << v << " ";
    std::cout << "\n";

    constexpr auto less_three = [](int v) { return v < 3; };
    // iterate over {3,4,5}
    for (auto v : data | std::views::drop_while(less_three))
        std::cout << v << " ";
    std::cout << "\n";
}
```
The std::plus, std::minus, std::multiplies, std::divides, std::modulus and std::negate are function objects from the &lt;functional&gt; header that wrap the corresponding arithmetic operators.

Since C++14, these objects also provide a void specialization that relies on type deduction for both arguments.
https://compiler-explorer.com/z/PWGYWKWcT

```c++

int main() {
    auto int_plus = std::plus<int>{};
    auto v = int_plus(4.2, 3.9);
    // decltype(v) == int, v == 7 (4+3)

    static_assert(std::is_same_v<decltype(v),int>);
    std::cout << "v == " << v << "\n";

    auto deduced_plus = std::plus<>{};
    auto w = deduced_plus(4, 3.9);
    // decltype(w) == double, w ~= 7.9

    static_assert(std::is_same_v<decltype(w),double>);
    std::cout << "w == " << w << "\n";

    // Useful when working with algorithms:
    std::vector<int> data{1,2,3,4,5};
    std::vector<int> out;
    std::ranges::transform(data,std::back_inserter(out),std::negate<>{});
    // out == {-1,-2,-3,-4,-5}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";

    auto product = std::ranges::fold_left(data,1,std::multiplies<>{});
    // product == 120

    std::cout << "product == " << product << "\n";
}
```
When working with C++20 views, creating functions that encapsulate complex view compositions might be tempting.

However, that will very likely lead to unnecessary data copies. Instead, consider compositing views as inline constexpr variables.
https://compiler-explorer.com/z/vG4hhe77h

```c++

constexpr inline auto trim_front = 
    std::views::drop_while([](int c) { return std::isspace(c); });
constexpr inline auto trim_back = 
    std::views::reverse | trim_front | std::views::reverse;
constexpr inline auto trim_space = 
    trim_back | trim_front;

int main() {
    std::string str = "    abc    \t";

    // Compose the view with data
    auto lazy_trimmed = str | trim_space;

    // Generate the output (before C++23 std::ranges::to)
    std::string out(lazy_trimmed.begin(), lazy_trimmed.end());
    // out == "abc"

    std::cout << "out == " << std::quoted(out) << "\n";
}
```
The std::rotate is a surprisingly useful, yet very simple algorithm.

The algorithm left-rotates the elements of the given range, so that the element under the iterator passed as the second argument ends up as the first element.
https://compiler-explorer.com/z/vs1e797f5

```c++

int main() {
    std::vector<int> data{1, 2, 3, 4, 5, 6, 7};

    // left-rotate the range by one element
    std::rotate(data.begin(), std::next(data.begin()), data.end());
    // data = { 2, 3, 4, 5, 6, 7, 1 }

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // rotate by aditional 3 elements
    std::rotate(data.begin(), std::next(data.begin(), 3), data.end());
    // data = { 5, 6, 7, 1, 2, 3, 4 }

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";
}
```
The typical use case for [[nodiscard]] is for marking functions whose returns should not be ignored. However, we can also apply [[nodiscard]] to constructors and entire types.

A nodiscard constructor shall not create ephemeral temporaries. When applied to a type, results of that type will be considered [[nodiscard]] even when not marked as such by the function.
https://compiler-explorer.com/z/336dKvMMn

```c++
void acquire_resource() {}
void release_resource() {}

struct ResourceHandle {
    // Nodiscard constructor
    [[nodiscard]] ResourceHandle() { acquire_resource(); };
    ~ResourceHandle() { release_resource(); }
};

// Nodiscard type
struct [[nodiscard]] Error {};

// nodiscard not needed, since Error is a nodiscard type
Error function() {
    return {};
}

int main() {
    // Warning, ignoring constructor declared with nodiscard
    ResourceHandle{}; // resource acquired and immediately released

    // Warning, ignoring return value marked with nodiscard
    function(); // ignoring Error
}
```
The std::views::reverse is a conceptually simple view that will produce a reversed view of the provided range.

The view requires a bidirectional range and will work even with uncommon ranges; however, remember that the view requires access to the end iterator, which can trigger eager evaluation.
https://compiler-explorer.com/z/zG77ne7sv

```c++

int main() {
    std::list<int> data{1,2,3,4,5,6};

    for (auto e : data | std::views::reverse) {
        // iterate over {6,5,4,3,2,1}
        std::cout << e << " ";
    }
    std::cout << "\n";

    // Reversing a views::reverse is guaranteed to be a noop
    auto noop = data | std::views::reverse | std::views::reverse;
    // noop.begin() == data.begin(), noop.end() == data.end()

    std::cout << std::boolalpha;
    std::cout << "noop.begin() == data.begin() : " << (noop.begin() == data.begin()) << "\n";
    std::cout << "noop.end() == data.end() : " << (noop.end() == data.end()) << "\n";

    // v is bidirectional range, but is not common
    // the end() returns a sentinel not an iterator
    auto v = data | std::views::take_while([](int v) { 
        return v <= 3; 
    });

    // OK, combining a bidirectional range with views::reverse
    auto w = v | std::views::reverse;

    // Linear operation, w.begin() forces v.end() to be calculated
    // to produce an iterator.
    auto value = *w.begin();
    // value == 3, w == {3,2,1}

    std::cout << "value == " << value << "\n";
    for (auto e : w)
        std::cout << e << " ";
    std::cout << "\n";
}
```
The C++11 std::addressof utility solves a very simple problem.

How do you obtain the actual address of an object when the built-in address-of operator can be overloaded?
https://compiler-explorer.com/z/PjqTPTKWe

```c++

struct X {
    X* operator&() {
        return nullptr;
    }
};

int main() {
    X x;

    // overload for operator& always returns nullptr
    assert(&x == nullptr);

    std::cout << "&x == " << &x << "\n";

    // actual address of the object
    assert(std::addressof(x) != nullptr);

    std::cout << "std::addressof(x) == " << std::addressof(x) << "\n";
}
```
The range-based loop was introduced in C++11; however, only with the introduction of C++20 views can it replace almost all instances for raw for-loops.
https://compiler-explorer.com/z/co8846fGs

```c++

int main() {
    // C++11: iterate over values in an initializer list
    for (auto v : {1, 1, 2, 3, 5, 8, 13})
        std::cout << v << " ";
    std::cout << "\n";

    // C++20: iterate over [1,10), values 1..9
    for (auto v : std::views::iota(1, 10))
        std::cout << v << " ";
    std::cout << "\n";

    // C++20: downward iteration 5..-5
    for (auto v : std::views::iota(-5, 6) | std::views::reverse)
        std::cout << v << " ";
    std::cout << "\n";

    std::vector<int> data{1, 2, 3, 4, 5, 6, 7, 8, 9};
    auto it = std::ranges::lower_bound(data, 5);

    // C++20: iterating over sub-range, 1..4
    for (auto v : std::ranges::subrange(data.begin(), it))
        std::cout << v << " ";
    std::cout << "\n";

    // C++20: iterating over sub-range, 5..9
    for (auto v : std::ranges::subrange(it, data.end()))
        std::cout << v << " ";
    std::cout << "\n";

    // C++23: nested loop replacement
    // iterate over i==[0,5), j==[0,5)
    for (auto [i,j] : std::views::cartesian_product(
                        std::views::iota(0,5),
                        std::views::iota(0,5)))
        std::cout << "i == " << i << ", j == " << j << "\n";
}
```
The final specifier can be applied to methods and classes.

When a method is marked as final, it cannot be overridden in derived classes.<br />When a class is marked as final, it cannot be derived from.

Note that final is not a reserved keyword, meaning that you can create variables and types with the name final. Although, in the case of types, that may lead to some confusion.
https://compiler-explorer.com/z/3afa95Tf6

```c++
struct A final {};

// Would not compile, A is marked as final.
struct B : A {};

struct X {
        virtual void method() = 0;
};
struct Y : X {
    void method() override final {} 
};

// Would not compile, B::method is marked as final.
struct Z : Y {
    void method() override {}
};

// final is not a reserved keyword
struct final final {}; // OK, struct final, which cannot be derived from

int main() {
    final f;
}
```
The C++17 Class Template Argument Deduction (CTAD) enables class template deduction from the constructor call.

This removes redundant type information, often improving readability and removing the potential for unintended implicit conversions.

Custom types can provide deduction guides (covered in a separate post), for when arguments do not match template parameters.
https://compiler-explorer.com/z/zdoq8E1Mq

```c++

// Simpler use of utility types
void utility() {
    std::mutex m;
    auto g = std::lock_guard(m);
    // Same as: auto g = std::lock_guard<std::mutex>(m);

    std::pair el{20, false};
    // Same as: std::pair<int, bool> el{20, false};

    std::vector data{1,2,3,4,5};
    // Same as std::vector<int> data{1,2,3,4,5};
    std::ranges::sort(data, std::greater{});
    // Same as std::ranges::sort(data, std::greater<void>{});
    // or std::ranges::sort(data, std::greater<>{});
}

// Simpler code inside of generic functions
auto function(auto&& x) {
    std::vector data(10, x);
    // Same as: std::vector<std::remove_cvref_t<decltype(x)>> data(10, x);
}

// Since C++20 also works for aggregate initialization
template <typename A, typename B, typename C>
struct SimpleType : A {
    B x;
    C y;
};

// and non-type template parameters
template <std::array Arr> 
struct NTTP {};

int main() {
    struct Base{};
    SimpleType a{Base{}, 10, 20};
    // Same as: SimpleType<Base,int,int> a{Base{}, 10, 20};

    using type = NTTP<{1,2,3,4,5}>;
    // Same as: using type = NTTP<std::array<int,5>{1,2,3,4,5}>;
}
```
Deduction guides are rules that instruct Class Template Argument Deduction (CTAD).

The compiler automatically generates guides that map each constructor call argument to a template parameter.

However, we can also provide custom guides to modify the default behaviour or trigger CTAD even when the constructor arguments don’t directly map to template parameters.
https://compiler-explorer.com/z/zqTvb9vas

```c++

template <typename T>
struct Storage {
    Storage(T t) : t_(t) {}

    template<std::input_iterator It>
    Storage(It begin, It end) : t_(begin,end) {}

    T t_;
};

// Constructor match -> Template instance
Storage(const char*) -> Storage<std::string>;

// Deduction guides can be templated
template<std::input_iterator It>
Storage(It,It) -> Storage<std::vector<
    typename std::iterator_traits<It>::value_type>>;

int main() {
    Storage a{10};
    // decltype(a) == Storage<int>
    // no guide required, deduced from Storage(T t)

    static_assert(std::is_same_v<decltype(a), Storage<int>>);

    Storage b{"Hello World!"};
    // decltype(b) == Storage<std::string>
    // follows the const char* guide

    static_assert(std::is_same_v<decltype(b), Storage<std::string>>);

    std::vector<int> data{1, 2, 3, 4, 5, 6};
    Storage c{data.begin(), data.end()};
    // Storage<std::vector<int>> - follows the double iterator guide

    static_assert(std::is_same_v<decltype(c), Storage<std::vector<int>>>);
}
```
The std::includes algorithm determines whether the second range is a (not necessarily contiguous) subsequence of the first range.

Both ranges must be sorted with respect to the same strict weak ordering. However, because of the sorted requirement, the algorithm operates in O(n).
https://compiler-explorer.com/z/WGhrKGzoE

```c++

int main() {
    std::vector<int> haystack{1,2,3,4,5,6,7,8,9,10};
    std::vector<int> needle1{3,4,9};

    bool v1 = std::includes(haystack.begin(), haystack.end(),
                            needle1.begin(), needle1.end());
    // v1 == true
    std::cout << std::boolalpha << "v1 == " << v1 << "\n";

    std::vector<int> needle2{3,3};
    bool v2 = std::includes(haystack.begin(), haystack.end(),
                            needle2.begin(), needle2.end());
    // v2 == false
    std::cout << std::boolalpha << "v2 == " << v2 << "\n";

    struct Item {
        int id;
        std::string label;
    };
    std::vector<Item> inventory{{0,"Banana"},{3,"Apple"},{4,"Cherry"},{5,"Melon"}};
    std::vector<Item> order{{3,"Apple"},{5,"Melon"}};

    // Check whether all items in the order are currently in the inventory,
    // however, only compare Item ids.
    bool all_in_stock = std::ranges::includes(inventory, order, {}, &Item::id, &Item::id);
    // all_in_stock == true
    std::cout << std::boolalpha << "all_in_stock == " << all_in_stock << "\n";
}
```
When working with large codebases, we often deal with deeply nested namespaces. This can be verbose and tedious.

C++17 introduced a simplified syntax for nested namespaces that compresses the specification into a single statement.
https://compiler-explorer.com/z/4YGv37e1j

```c++
// Before C++17
namespace LibraryName {
    namespace ComponentName {
        namespace ModuleName {
            struct SomeType {};
        }
    }
}

// Since C++17
namespace LibraryName::ComponentName::ModuleName {
    struct SomeOtherType {};
}

int main() {
    auto a = LibraryName::ComponentName::ModuleName::SomeType{};
    auto b = LibraryName::ComponentName::ModuleName::SomeOtherType{};
}
```
The C++20 std::views::filter produces a view of elements from the underlying range that satisfy the provided predicate (skipping over those that do not).

The view models up to a bidirectional range (based on the underlying range&#39;s properties) and supports a common range interface (if provided by the underlying range).
https://compiler-explorer.com/z/vn1KTK494

```c++

int main() {
    std::vector<int> data{1,2,3,4,5,6,7};
    
    auto filtered = data | std::views::filter([](int v) {
        return v % 2 == 0;
    });
    // filtered is a bidirectional common range
    std::vector<int> out(filtered.begin(), filtered.end());
    // out == {2,4,6}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";

    auto uncommon = std::views::iota(1) | std::views::take(7) | 
        std::views::filter([](int v) {
            return v % 2 == 0;
        });
    // uncommon is a bidirectional uncommon range
    
    // Wouldn't compile (requires a common range)
    //std::vector<int> out2(uncommon.begin(), uncommon.end());
    std::vector<int> out3;
    std::ranges::copy(uncommon, std::back_inserter(out3));
    // out3 == {2,4,6}

    for (auto v : out3)
        std::cout << v << " ";
    std::cout << "\n";
}
```
The C++23 standard introduced a monadic interface for std::optional&lt;T&gt;.

- transform(f(T)-&gt;U)-&gt;std::optional&lt;U&gt;

  only invoked when the source isn&#39;t empty

- and_then(f(T)-&gt;std::optional&lt;U&gt;)-&gt;std::optional&lt;U&gt;

  only invoked when the source isn&#39;t empty

- or_else(f()-&gt;std::optional&lt;T&gt;)-&gt;std::optional&lt;T&gt;

  only invoked when the source is empty
https://compiler-explorer.com/z/4dzc776Gq

```c++

std::string stringify(int v) {
    return std::to_string(v);
}

int integerify(const std::string& s) {
    return std::stoi(s);
}

std::optional<int> fetch_from_db() {
    return 42;
}

int main() {
    // std::optional<>::tranform() a wrapper for transformation functions
    std::optional<int> x;
    auto r1 = x.transform(stringify)
               // does not invoke stringify
               // returns empty std::optional<std::string>
               .transform(integerify); 
               // does not invoke integerify
               // returns empty std::optional<int>
    // r1 == std::nullopt

    assert(r1 == std::nullopt);

    x = 42;
    auto r2 = x.transform(stringify)
               // invokes stringify on int{42}
               // returns std::optional{"42"};
               .transform(integerify);
               // invokes integerify on std::string{"42"}
               // returns std::optional{42};
    // r2 == int{42}

    assert(r2 == 42);

    // std::optional<>::and_then() a wrapper for a transformation function
    // that returns an optional (i.e. can return an empty one)
    // Transformation is invoked only when the optional is not empty.
    auto r3 = x.and_then([](int& x) {
        if (x == 42)
            return std::optional<std::string>("Answer to the Ultimate Question of Life, the Universe, and Everything");
        else
            return std::optional<std::string>{};
    });
    // r3 == std::string{"Answer to the Ultimate Question of Life, the Universe, and Everything"}

    assert(r3 == "Answer to the Ultimate Question of Life, the Universe, and Everything");

    // std::optional<>::or_else a wrapper for a generation function
    // that returns an optional.
    // The function is only invoked when the optional is empty.
    x = std::nullopt;
    auto r4 = x.or_else(fetch_from_db);
    // r4 == int{42}

    assert(r4 == 42);

    // More involved example:
    auto r5 = fetch_from_db().or_else([](){
        // Invoked if fetch_from_db returns an empty optional
        return std::optional<int>{42}; 
    }).transform([](int x) -> double{
        // Invoked on the result of fetch_from_db or or_else
        // converts to double.
        return x*x;
    }).and_then([](double x){
        // Invoked on the result of transform and because we 
        // have preceeding or_else this will always be invoked.
        if (x > 0) {
            return std::optional<std::string>{"42"};
        } else {
            return std::optional<std::string>{};
        }
    }).or_else([](){
        // Invoked if and_then returned an empty optional
        return std::optional<std::string>{"Empty"};
    });
    // r5 == std::string{"42"}

    assert(r5 == std::string{"42"});
}
```
The std::any, introduced in C++17, will hold a single copy-constructible value of any type.

std::any can be treated as a type-safe alternative to void*, with the caveat (and benefit) that std::any provides value semantics.
https://compiler-explorer.com/z/jEYPvfnE8

```c++

struct BasicPayload {};
struct ExtendedPayload {};

void function(std::any payload) {
    if (payload.type() == typeid(BasicPayload)) {
        // Process basic payload
    } else if (payload.type() == typeid(ExtendedPayload)) {
        // Process extended payload
    } else {
        throw std::runtime_error("Bad payload type.");
    }
}

int main() {
    function(BasicPayload{});
    function(ExtendedPayload{});
    // function(10); // throws std::runtime_error

    std::any holder; // Empty std::any
    assert(holder.has_value() == false);

    holder = 42;
    try {
        // OK, holder holds an int, returns copy
        int v = std::any_cast<int>(holder);

        // Also OK, obtain pointer to contained data
        int *p = std::any_cast<int>(&holder);

        // Not OK, will throw bad_any_cast
        // double w = std::any_cast<double>(holder);
    } catch (const std::bad_any_cast&) {}
}
```
The std::sort is perhaps one of the most well-known algorithms.

The algorithm sorts elements (by default in non-descending order) and doesn&#39;t maintain the relative order of equivalent elements.

C++17 standard added a parallel variant.<br />The C++20 standard added a range version and enabled constexpr evaluation for all but the parallel variant.
https://compiler-explorer.com/z/Y9fWYdG9a

```c++

int main() {
    std::vector<int> data{9,2,6,4,3,5,1,7,8};

    // Default sort using operator<
    std::sort(data.begin(), data.end());
    // data == {1,2,3,4,5,6,7,8,9}
    
    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // Sort with custom comparator
    std::sort(data.begin(), data.end(), std::greater<>{});
    // data == {9,8,7,6,5,4,3,2,1}

    // Same as above, but using a lambda
    std::sort(data.begin(), data.end(), [](int l, int r) { return l > r; });
    // data == {9,8,7,6,5,4,3,2,1}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // C++17 parallel version
    std::sort(std::execution::par_unseq, data.begin(), data.end());
    // data == {1,2,3,4,5,6,7,8,9}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // C++20 range version
    // Note: default comparator is std::ranges::less{}, not operator<
    std::ranges::sort(data, std::ranges::greater{});
    // data == {9,8,7,6,5,4,3,2,1}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n\n";

    struct Item {
        int id;
        std::string label;
    };
    std::vector<Item> words{{9, "bench"}, {2, "rain"}, {0, "reasonable"}, {5, "fool"}, {3, "waist"}, {7, "insure"}};

    // Sort lexicographically by label
    std::ranges::sort(words, {}, &Item::label);
    // Same as: std::ranges::sort(words, std::ranges::less{}, &Item::label);
    // {9:"bench", 5:"fool", 7:"insure", 2:"rain", 0:"reasonable", 3:"waist"}

    for (const auto& v : words)
        std::cout << v.id << ": " << std::quoted(v.label) << "\n";
    std::cout << "\n";

    // Sort by id, non-ascending
    std::ranges::sort(words, std::ranges::greater{}, &Item::id);
    // {9:"bench", 7:"insure", 5:"fool", 3:"waist", 2:"rain", 0:"reasonable"}

    for (const auto& v : words)
        std::cout << v.id << ": " << std::quoted(v.label) << "\n";
}
```
Calculating the midpoint value between two arithmetic types or pointers might seem trivial; however, when the values are close to numerical limits or not in order, trivial implementations can easily run into undefined behaviour.

C++20 introduced std::midpoint, which provides a safe implementation.


```c++

```
The C++20 std::views::take and std::views::take_while produce views of the leading elements of the adapted range.

Additionally, std::views::take is optimized for random access ranges and will not introduce any overhead for std::span, std::string_view, std::views::iota, std::views::repeat and std::ranges::subrange (if it models random_access and sized).
https://compiler-explorer.com/z/Efv174Yaf

```c++

int main() {
    std::vector<int> data{1,2,3,4,5}; 

    // First three elements of data
    auto x = data | std::views::take(3);
    // x == {1,2,3}
    // decltype(x) == std::ranges::take_view<std::vector<int>>

    for (auto e : x)
        std::cout << e << " ";
    std::cout << "\n";

    // Taking more elements than available is safe
    auto y = data | std::views::take(42);
    // std::size(y) == 5

    std::cout << "std::size(y) == " << std::size(y) << "\n";

    // Optimized for span, string_view, views::iota, 
    // views::repeated, ranges::subrange.
    auto z = std::span(data) | std::views::take(3);
    // z == {1,2,3}
    // decltype(z) == std::span<int>

    // Take until the predicate evaluates as false
    auto v = data | std::views::take_while([](int i) {
        return i % 5 != 0;
    });
    // v == {1,2,3,4}

    for (auto e : v)
        std::cout << e << " ";
    std::cout << "\n";
}
```
Single-argument constructors and conversion operators should (almost) always be marked as explicit to prevent accidental silent conversions.

When the conversion is desired, explicit operations can be invoked by spelling out the destination type.


```c++

```
The two algorithms, std::replace and std::replace_if, will replace elements of a range that match the provided value or predicate with a new provided value.

Both algorithms are constexpr enabled (C++20) and have parallel (C++17) and range (C++20) variants.
https://compiler-explorer.com/z/hK4azaKeP

```c++

struct Cycle {
    enum { VALID, EXPIRED, READY } state;
    std::string data;
};

int main() {
    std::vector<int> data{1,2,3,4,5,6};
    // Replace elements matching the value with 
    // a different value.
    std::replace(data.begin(), data.end(), 2, 8);
    // data == {1, 8, 3, 4, 5, 6}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    std::vector<Cycle> runners{
        {Cycle::VALID,"hello"},
        {Cycle::EXPIRED,"bye"},
        {Cycle::READY,"i'm ready"}
        };

    // With C++20 projections, we can project each element
    // and compare against the projected value.
    std::ranges::replace(runners,
        Cycle::EXPIRED, // if equal to this
        Cycle{Cycle::READY, "let's go"}, // replace with this
        &Cycle::state); // project each element to the member state
    // runners == {{VALID,"hello"},{READY,"let's go"},{READY,"i'm ready"}}

    for (auto &[state, data] : runners)
        std::cout << int{state} << " " << data << "\n";

    // Replace elements for which the predicate evaluates to true.
    std::replace_if(data.begin(), data.end(),
        [](int v) { return v % 2 == 0; }, 
        -1);
    // data == {1, -1, 3, -1, 5, -1}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";
}
```
The typeid operator can be applied to types and expressions and will return a reference to a std::type_info object representing the type (of the expression).

When applied to polymorphic types (except during construction and destruction), the typeid operator will evaluate the expression and return the dynamic type of the expression.
https://compiler-explorer.com/z/YxePzhbhE

```c++

struct Base {
    virtual void make_me_polymorphic() {}
};

struct Derived : Base {};

void inspector(Base& b) {
    // Base is a polymorphic type (constains a virtual method)
    if (typeid(b) == typeid(Base)) {
        std::cout << "Called with Base.\n";
    } else {
        std::cout << "Called with Derived.\n";
    }
}

int main() {
    // Get an implementation defined name of this type:
    std::string_view int_name = typeid(int).name();
    std::cout << "typeid(int).name() == " << int_name << "\n";
    // On GCC: "i"

    // Same for an expression:
    std::string_view double_name = typeid(3+2.1).name();
    std::cout << "typeid(3+2.1).name() == " << double_name << "\n";
    // On GCC: "d"

    Base x;
    Derived y;
    inspector(x); // prints: "Called with Base"
    inspector(y); // prints: "Called with Derived"
}
```
The std::min_element, std::max_element and (C++11) std::minmax_element are min-max algorithms that operate on top of iterators, returning an iterator to the minimum/maximum element.

The algorithms provide parallel (C++17) variants and are constexpr and range enabled (C++20).

C++20 also offers a simpler alternative: a range overload of the base min-max algorithms.
https://compiler-explorer.com/z/e4aE6c8Pf

```c++

int main() {
    std::vector<int> data{1,2,3,4,5};

    auto min = std::min_element(data.begin(), data.end());
    // min == data.begin(), *min == 1

    std::cout << "*min == " << *min << "\n";
    
    auto max = std::max_element(data.begin(), data.end());
    // max == std::prev(data.end()), *max == 5

    std::cout << "*max == " << *max << "\n";

    auto [mi,ma] = std::minmax_element(data.begin(), data.end());
    // mi == min, ma == max, *mi == 1, *ma == 5

    std::cout << "*mi == " << *mi << ", *ma == " << *ma << "\n";
    
    // If we only need the values, C++ 20 offers a simpler alternative:
    auto [x,y] = std::ranges::minmax(data); // Returns by-value
    // x == 1, y == 5
    
    std::cout << "x == " << x << ", y == " << y << "\n";

    // Example with projections:
    struct Element {
        int v;
    };

    std::vector<Element> elements{{2},{1},{4},{5},{3}};
    // Select the minimum element based on the value of Element::v
    auto it = std::ranges::min_element(elements, {}, &Element::v);
    // *it == Element{1}

    std::cout << "it->v == " << it->v << "\n";
}
```
C++17 filesystem library introduced the notion of a filesystem path.

Paths can be converted to their absolute, canonical and relative forms and tested for equivalence across all these types.

Paths that refer to directories can be explored using directory iterators either only for that directory or recursively.
https://compiler-explorer.com/z/3G7Gvn4T6

```c++

int main() {
    std::filesystem::path local(".");
    // iterate over entries in directory specified by path
    for (const auto& entry : std::filesystem::directory_iterator(local)) {
        auto p1 = entry.path(); // e.g. ./file.ext
        auto p2 = absolute(entry.path()); // e.g. /some/path/./file.ext
        auto p3 = canonical(entry.path()); // e.g. /some/path/file.ext

        std::cout << "default: " << p1 << "\n";
        std::cout << "absolute: " << p2 << "\n";
        std::cout << "canonical: " << p3 << "\n\n";

        // paths can be checked for equivalence (only for valid paths)
        assert(equivalent(p1,p2));
        assert(equivalent(p2,p3));
    }

    // recursively iterate over entries in directory specified by path
    for (const auto& entry : std::filesystem::recursive_directory_iterator(local)) {
        auto p1 = entry.path(); // e.g. ./file.ext
        auto p2 = absolute(entry.path()); // e.g. /some/path/./file.ext
        auto p3 = canonical(entry.path()); // e.g. /some/path/file.ext
    }

    // Relative paths
    std::filesystem::path a("/some/file/path");
    std::filesystem::path b("/some/other/path");

    std::filesystem::path c = relative(a,b);
    // c == "../../file/path"

    std::cout << "c == " << c << "\n";
}
```
The std::partition algorithm reorders elements of a range based on a predicate. Elements for which the predicate evaluates to true are all ordered before elements for which the predicate evaluates to false.

The stable version std::stable_partition also maintains the relative order of elements (within each partition).
https://compiler-explorer.com/z/e68oPcjrn

```c++

int main() {
    std::vector<int> data1{1, 2, 3, 4, 5, 6, 7, 8, 9};

    auto pp1 = std::partition(data1.begin(), data1.end(), 
        [](int v) { return v % 2 == 0; }); // predicate for even numbers

    // [begin, partition_point) - even elements
    for (auto it = data1.begin(); it != pp1; ++it)
        std::cout << *it << " ";
    std::cout << "\n";

    // [partition_point, end) - odd elements
    for (auto it = pp1; it != data1.end(); ++it)
        std::cout << *it << " ";
    std::cout << "\n\n";

    std::vector<int> data2{1, 2, 3, 4, 5, 6, 7, 8, 9};

    auto odd = std::ranges::stable_partition(data2,
        [](int v) { return v % 2 == 0; }); // predicate for even numbers

    // Range version returns a sub-range {partition_point, end}
    // guaranteed order {1, 3, 5, 7, 9}
    for (auto v : odd)
        std::cout << v << " ";
    std::cout << "\n";

    // even elements
    auto even = std::ranges::subrange(data2.begin(), odd.begin());
    // guaranteed order {2, 4, 6, 8}  
    for (auto v : even)
        std::cout << v << " ";
    std::cout << "\n";
}
```
The std::sample algorithm (C++17) is a stable (maintains relative order of elements) sampling algorithm that randomly copies the specified number of elements from the source range into the destination range (output iterator).
https://compiler-explorer.com/z/EWazzMbns

```c++

int main() {
    std::vector<int> data{1, 2, 3, 4, 5, 6, 7, 8, 9};
    std::vector<int> out;

    auto gen = std::mt19937(1); // fixed seed for deterministic result

    std::sample(data.begin(), data.end(),   // input range
        std::back_inserter(out),            // output iterator
        4,      // number of elements to sample
        gen);   // random number engine

    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";

    // stdlibc++: out == {1, 6, 8, 9}
    // libc++: out == {2, 4, 5, 9}
}
```
The std::deque is a non-contiguous container that models a double-ended queue while still providing random access, albeit at the cost of one extra indirection.

Elements can be added and removed from each end of the queue with O(1) time complexity without invalidating existing iterators (except for the erased elements).
https://compiler-explorer.com/z/jfjaTsKxE

```c++

int main() {
    std::deque<int> data;

    // On top of push_back and pop_back we also get the same for front
    data.push_back(1);
    // data == {1}
    data.push_front(2);
    // data == {2, 1}
    data.push_back(3);
    // data == {2, 1, 3}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    data.pop_front();
    data.pop_front();
    // data == {3}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // Otherwise the std::deque offers the same interface as std::vector
    data.resize(7, 42);
    // data == {3, 42, 42, 42, 42, 42, 42}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // Linear operations:
    data.insert(data.begin()+1, 7);
    // data == {3, 7, 42, 42, 42, 42, 42, 42}
    data.erase(data.begin()+2);
    // data == {3, 7, 42, 42, 42, 42, 42}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";
}
```
The C++23 std::view::adjacent_transform is a view that produces elements by continually applying the provided N-ary invocable to each consecutive group of N elements.

The functionality can be simulated by combining std::views::adjacent and std::views::transform, however, this is both less efficient and more cumbersome.
https://compiler-explorer.com/z/Ys1WEdq9W

```c++

int main() {
    std::vector<int> data{5,1,2,4,3};

    auto med3 = [](int a, int b, int c) {
        if (a >= b) {
            if (b >= c) return b;
            if (a >= c) return c;
            return a;      
        } else {
            if (c >= b) return b;
            if (a >= c) return a;
            return c;
        }
    };

    auto medians = data | std::views::adjacent_transform<3>(med3);
    // medians == {2, 2, 3}

    for (auto e : medians)
        std::cout << e << " ";
    std::cout << "\n";    

    // Same as the following, but avoiding the intermediate tuple:
    auto medians_twostep = data | std::views::adjacent<3> | 
        std::views::transform([&](auto&& e) {
            return std::apply(med3, e);
        });
    // medians_twostep == {2, 2, 3}

    for (auto e : medians_twostep)
        std::cout << e << " ";
    std::cout << "\n";

    // Simulating the adjacent difference algorithm.
    // The pairwise specialization is equivalent to adjacent_transform<2>.
    auto adjacent_difference = data |
        std::views::pairwise_transform(std::minus<>{});
    // adjacent_difference == {4, -1, -2, 1}
    
    for (auto e : adjacent_difference)
        std::cout << e << " ";
    std::cout << "\n";
}
```
The greatest common divisor and least common multiple are often used as simple programming exercises or interview questions.

Since C++17, we finally have the standard versions of these functions as std::gcd and std::lcm in the numeric header.
https://compiler-explorer.com/z/a48Yexbrd

```c++

int main() {   
    auto gcd = std::gcd(2*3, 3*5);
    // gcd == 3

    std::cout << "gcd(2*3, 3*5) == " << gcd << "\n";

    auto lcm = std::lcm(2*3, 3*5);
    // lcm == 2*3*5

    std::cout << "lcm(2*3, 3*5) == " << lcm << "\n";
}
```
The std::remove and std::remove_if compact a range so that the leading sub-range [begin, last) does not contain any elements that match the provided value or for which the predicate evaluates to true.

The number of elements in the range is unaffected, the non-removed elements maintain their relative order, and the subrange [last, end) may contain elements in a moved-from state.
https://compiler-explorer.com/z/65zoKv7c1

```c++

int main() {
    std::vector<int> data{1,2,3,4,5};
    auto it = std::remove(data.begin(), data.end(), 3);

    // [begin, it) contains non-removed elements
    auto remainder = std::ranges::subrange(data.begin(), it);
    // remainder == {1,2,4,5}

    for (auto v : remainder)
        std::cout << v << " ";
    std::cout << "\n";

    // Range version since C++20, returns the [it, end) subrange
    auto removed = std::ranges::remove_if(remainder,
        [](int v) { return v % 2 == 0; });
    auto odd = std::ranges::subrange(remainder.begin(), removed.begin());
    // odd == {1,5}

    for (auto v : odd)
        std::cout << v << " ";
    std::cout << "\n";

    // Move only object that sets its value to "empty" when moved from
    struct Object {
        Object(std::string value) : value(value) {}
        Object(Object&& other) : value(std::exchange(other.value,"empty")) {}
        Object& operator=(Object&& other) { 
            value = std::exchange(other.value,"empty");
            return *this;
        }
        std::string value;
    };

    std::vector<Object> move_semantics;
    move_semantics.emplace_back("hello");
    move_semantics.emplace_back("this");
    move_semantics.emplace_back("is");
    move_semantics.emplace_back("dog");

    std::ranges::remove_if(move_semantics,
        [](auto &e) { return e.value.length() > 4; });
    // move_semantics == {{"this"},{"is"}, {"dog"}, {"empty"}}

    for (auto &v : move_semantics)
        std::cout << v.value << " ";
    std::cout << "\n";
}
```
The std::list and std::forward_list are containers with perfect iterator stability and support for O(1) splicing of elements.

The only situation that invalidates an iterator is the removal of elements, and only for the removed elements, even when splicing between containers.

Consequently, traversing std::list and std::forward_list is roughly 5x-10 slower than std::vector.
https://compiler-explorer.com/z/reaoEYY8n

```c++

void print(const auto& rng) {
    for (auto v : rng)
        std::cout << v << " ";
    std::cout << "\n";
}

int main() {
    std::list<int> data{1,2,3,4,5};
    std::list<int> other{6,7,8,9};

    // Get iterator to the third element.
    // Note, this is a linear operation:
    // i.e. auto it = std::next(std::next(data.begin()));
    auto it = std::next(data.begin(), 2);

    // Transfer the third element of data before the first element of other
    other.splice(other.begin(), data, it); // O(1) operation
    // it still valid, *it == 3
    // data == {1, 2, 4, 5}, other == {3, 6, 7, 8, 9}

    std::cout << "*it == " << *it << "\n";

    std::cout << "data == ";
    print(data);
    std::cout << "other == ";
    print(other);

    // std::list provides only bidirectional and std::forward_list
    // only forward iterator support.

    // But both provide common algorithms as methods:
    std::forward_list<int> list{5,2,1,4,3};

    list.sort(); // approx. n*logn
    // list == {1, 2, 3, 4, 5}
    std::cout << "list == ";
    print(list);

    list.reverse(); // O(n)
    // list == {5, 4, 3, 2, 1}
    std::cout << "list == ";
    print(list);

    // std::forward_list provides an unusual interface due to forward list
    // structure (can only insert/erase after an element).
    list.insert_after(list.before_begin(), 42); // O(1)
    // same as list.push_front(42);
    // list == {42, 5, 4, 3, 2, 1}
    std::cout << "list == ";
    print(list);

    list.erase_after(list.begin());
    // list == {42, 4, 3, 2, 1}
    std::cout << "list == ";
    print(list);
}
```
Before C++20, erasing elements from most containers based on their value or a predicate was a two-step process.

C++20 added a set of container-specific function overloads of std::erase and std::erase_if. These functions simplify the two-step process into a single call and (for the value version) support heterogeneous comparisons.
https://compiler-explorer.com/z/n1GKrzKEY

```c++

int main() {
    std::vector<int> data{1,2,3,4,5};

    std::erase(data, 3);
    // same as:
    auto it = std::remove(data.begin(), data.end(), 3);
    data.erase(it, data.end());
    // data == {1, 2, 4, 5}

    std::cout << "data == ";
    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    std::erase_if(data, [](int v) { return v % 2 == 0; });
    // same as:
    it = std::remove_if(data.begin(), data.end(), [](int v) { return v % 2 == 0; });
    data.erase(it, data.end());

    std::cout << "data == ";
    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // Additionally std::erase(_if) returns the number of erased elements:
    size_t cnt = std::erase_if(data, [](int v){ return v != 0; });
    // cnt == 2, data == {}

    std::cout << "cnt == " << cnt << "\n";

    std::cout << "data == ";
    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // Supported for:
    // std::vector, std::deque, std::list, std::forward_list, std::string

    // Heterogeneous comparison:
    std::vector<std::string> mixed{"hello","this","is","dog"};
    std::erase(mixed, "hello");
    // "hello" directly compared with each element without conversion
    // mixed == {"this", "is", "dog"}

    // unordered and associative containers only support std::erase_if
    std::map<int,int> map{{1,2},{2,3},{3,4}};
    std::erase_if(map, [](auto &e) { return e.first < 3; });
    // map == {3->4}

    std::cout << "map == ";
    for (auto &[k,v] : map)
        std::cout << k << "->" << v << " ";
    std::cout << "\n";
    // Note that map::erase() already supports heterogeneous erase-by-value
}
```
The std::initializer_list (C++11) is a simple proxy object around a const-array that is automatically constructed when a braced-initializer-list is:

- used as a function argument<br />- used to initialize or assign to an object with an appropriate constructor/assignment operator<br />- bound to auto

Note that we cannot move from a const array, which has performance implications.
https://compiler-explorer.com/z/WjzETocx7

```c++

struct X {
    X(std::initializer_list<int>) {}
    X& operator=(std::initializer_list<int>){ return *this; }
};

void function(std::initializer_list<int>) {}

struct Data{
    Data() { std::cout << "default constructor\n"; }
    Data(const Data&) { std::cout << "copy constructor\n"; }
    Data(Data&&) { std::cout << "move constructor\n"; }
    Data& operator=(const Data&) { std::cout << "copy assignment\n"; return *this; }
    Data& operator=(Data&&) { std::cout << "move assignment\n"; return *this; }
    ~Data() { std::cout << "destructor\n"; }
};

int main() {
    // Constructing an object
    X x{1, 2, 3, 4, 5};
    // Assigning to an object
    x = {1, 2, 3, 4, 5};

    // Function explicitly accepting initializer_list
    function({1, 2, 3, 4, 5});

    // Binding to auto
    auto y = {1, 2, 3, 4, 5};
    // decltype(y) == std::initializer_list<int>

    {
    // initialized by copy-initialization
    Data a, b;
    std::vector<Data> z{a, b};
    // 4x copy
    //      2x to create std::initializer_list
    //      2x to create std::vector
    }
    std::cout << "\n\n";
    {
    // C++17 guaranteed copy-elision
    std::vector<Data> w{Data{}, Data{}};
    // copy-initialization from pr-values turns into direct-initialization
    // 2x copy to create std::vector
    }    
}
```
Curiously Recurring Template Pattern (CRTP) is a C++ technique allowing us to pass type information about a derived class to a base class.

A typical use case for CRTP is Mixin support, where we encapsulate functionality into a base class without relying on virtual dispatch. This way, the derived class can avoid becoming polymorphic and potentially remain trivially copyable.
https://compiler-explorer.com/z/xo3haE7Tc

```c++

struct Connection{};
struct ServerConfig{};

// The Base/Mixin type
template <typename Handler>
struct SimpleServer {
    // Public interface, exposed through the derived class.
    void listen() {
        bool running = true;
        while (running) {
            Connection conn = accept_connection();
            // Safe, because we know that Handler is a derived class.
            static_cast<Handler*>(this)->handle_connection(conn);
            // Just to be friendly to Compiler Explorer:
            running = false;
        }
    }
protected:
    // Protected interface, only usable by the derived class.
    int read(Connection, std::span<std::byte>) { /* impl */ return 0; }
    int write(Connection, std::span<std::byte>) { /* impl */ return 0; }
    // Prevent accidental direct instantiation
    SimpleServer(ServerConfig cfg) { }
private:
    Connection accept_connection() { return {}; }
};

// Users only need to initialize and implement the handle method.
struct ServerImpl : SimpleServer<ServerImpl> {
    ServerImpl() : SimpleServer<ServerImpl>(ServerConfig{}) {}

    void handle_connection(Connection conn) {
        std::vector<std::byte> buff(64*1024);
      	// SimpleServer<SererImpl>::read()
        int rres = read(conn, buff);
      
        /* process data */
      
       	// SimpleServer<SererImpl>::write()
        int wres = write(conn, buff);
    }
};


int main() {
    ServerImpl server;
    server.listen();
}
```
Argument Dependent Lookup (ADL) governs how unqualified function calls are resolved.

After looking up a function in the current (and parent) namespace(s), the innermost namespaces of the types of arguments (simplified) will also be considered.

Due to interactions with visibility rules, we can set up functions (callables) to only be invocable through ADL or qualified calls.
https://compiler-explorer.com/z/7ha8Gcjn7

```c++
namespace lib1 {
    struct X {
        friend void adl_only(const X&) {}
    };
    X operator+(const X&, const X&) { return {}; }
    void operate(const X&) {}
    void shut_off(const X&) {}
    constexpr inline auto only_explicit = [](const X&) {};
}

constexpr inline auto shut_off = [](auto&&) {};

int main() {
    lib1::X a,b;
    // Operator overloading requires ADL.
    a = a + b; // Syntax sugar for: a = operator+(a, b);
    // Without operator overload we would have to write:
    a = lib1::operator+(a, b);

    // ADL call, lookup finds lib1::operate()
    // because decltype(a) == lib1::X
    operate(a);
    // Qualified call still works.
    lib1::operate(a);

    // Non-function entities shut off ADL, this calls ::shut_off()
    shut_off(a);
    // Qualified call still works.
    lib1::shut_off(a);

    // Friend functions are members of the surrounding namespace,
    // but are not visible outside of ADL.
    adl_only(a);
    // lib1::adl_only(a); // Will not compile, not visible outside of ADL.

    // Non-function entities also do not participate in ADL.
    // only_explicit(a); // Will not compile not visible to ADL.
    // Qualified call still works.
    lib1::only_explicit(a);
}
```
The std::expected is a C++23 type representing either a correct result or an unexpected error.

Semantically, std::expected works very similarly to std::optional; instead of optionally storing a value, std::expected always stores either a result or an error.
https://compiler-explorer.com/z/3eaEW7Yzr

```c++


// Typical use case
std::expected<std::string,std::error_code> fun() {
    bool error = true;
    if (error)
        return std::unexpected(std::make_error_code(std::errc::invalid_argument));
    return "Hello World!";
}

int main() {
    // Similar interface to std::optional
    std::expected<int,int> v = 10;
    // v.has_value() == true
    // v.value() == 10, *v == 10

    std::cout << std::boolalpha << "v.has_value() == " << v.has_value() << "\n";
    std::cout << "*v == " << *v << ", v.value() == " << v.value() << "\n";

    // To distinguish the error, it has to be wrapped in std::unexpected
    std::expected<int,int> e = std::unexpected{10};
    // e.has_value() == false
    // e.error() == 10

    std::cout << std::boolalpha << "e.has_value() == " << e.has_value() << "\n";
    std::cout << "e.error() == " << e.error() << "\n";

    // std::expected always contains either a result or an error
    std::expected<int,int> m;
    // m.has_value() == true
    // m.value() == int{} == 0

    std::cout << "m.has_value() == " << m.has_value() << "\n";
    std::cout << "m.value() == " << m.value() << "\n";

    // Therefore if the result type cannot be default constructed
    // the resulting std::expected cannot be default constructed either.
    struct NoDefault {
        NoDefault(int) {}
    };
    // std::expected<NoDefault,int> n; // Wouldn't compile
    std::expected<NoDefault,int> n{20}; // OK
    // n.has_value() == true

    std::cout << "n.has_value() == " << n.has_value() << "\n";
}
```
The std::views::counted is the C++20 range equivalent to the std::counted_iterator, producing a range from an iterator and the number of elements.

Similar to std::views::take, this view also handles contiguous and random-access-sized ranges efficiently.

For contiguous ranges, it produces a std::span; for random-access-sized ranges, it resolves the end iterator.
https://compiler-explorer.com/z/5nsrsqxTx

```c++

int main() {
    std::vector<int> data{1,2,3,4,5};

    auto c = std::views::counted(data.begin(), 3);
    // decltype(c) == std::span<int>
    // c == {1,2,3}

    static_assert(std::is_same_v<decltype(c), std::span<int>>);
    for (auto v : c)
        std::cout << v << " ";
    std::cout << "\n";

    std::deque<int> q{1,2,3,4,5};
    auto random = std::views::counted(q.begin(), 3);
    // random == {1,2,3}
    // *random.end() == 4, O(1) operation

    std::cout << "*random.end() == " << *random.end() << "\n";
    for (auto v : random)
        std::cout << v << " ";
    std::cout << "\n";

    // More involed example:

    // Preallocate space for three elements.
    std::vector<int> top_three(3);  
    // Produce the lowest three elements in top_three.
    std::ranges::partial_sort_copy( 
        // From upto 5 integers read from the standard input.
        std::views::counted( 
            std::istream_iterator<int>(std::cin), 5),
        top_three
    );
    // For input: 13 97 42 7 666 1
    // top_three == {7, 13, 42}

    for (auto v : top_three)
        std::cout << v << " ";
    std::cout << "\n";
}
```
One of the possibilities to introduce a customization point in a library is through ADL (Argument Dependent Lookup). With C++20 concepts, this approach got much cleaner.

A Niebloid combined with a concept that detects the presence of custom implementation can handle the fallback to default implementation without the need to fiddle with namespaces on the calling site.
https://compiler-explorer.com/z/r3vcaPWM6

```c++

namespace dflt {
namespace impl {

// Concept checking whether an ADL call is valid.
template <typename T> 
concept HasADL = requires(T a) { do_something(a); };

// Main machinery to pick between default or ADL implementation.
struct DoSomethingFn {
    // Type has a custom implementation, simply call, relying on ADL.
    template <typename T> void operator()(T&& arg) const
    requires HasADL<T> { do_something(std::forward<T>(arg)); }

    // Type doesn't have a custom implementation, use the default.
    template <typename T> void operator()(T&&) const
    requires (!HasADL<T>) {
        /* default implementation */
        std::cout << "Default Implementation\n";
    }
};    
}
// Inline namespace makes the inline variable not conflict with 
// implementations for types in dflt namespace.
inline namespace var {
// Only invocable as dflt::do_something, not visible to ADL.
constexpr inline auto do_something = impl::DoSomethingFn{};
}
}

namespace lib {
struct X {
    friend void do_something(const X&){ std::cout << "Customized for X\n"; }; 
};
}
namespace dflt {
struct Y {
    friend void do_something(const Y&){ std::cout << "Customized for Y\n"; };
};
}

int main() {
    int a = 0; lib::X x; dflt::Y y;
    // No explicit implementation for int, falls back to default.
    dflt::do_something(a);
    // Calls the friend function do_something for X
    dflt::do_something(x);
    // Calls the friend function do_something for Y
    dflt::do_something(y);

    // Pure-ADL calls, will only invoke custom implementations.
    do_something(x);
    do_something(y);
}
```
The std::binary_search is a presence-check algorithm that operates on at least partially ordered ranges and provides O(logn) complexity if the range models at least random-access iteration.

The algorithm does have a range variant and supports constexpr (both since C++20); however, as might be expected, it does not have a parallel variant.
https://compiler-explorer.com/z/dzPPefeos

```c++

int main() {
    std::vector<int> data{1,2,3,4,5,6,7,8,9};

    // Default version with operator< as comparator
    bool contains = std::binary_search(data.begin(), data.end(), 4);
    // contains == true

    std::cout << std::boolalpha << "contains == " << contains << "\n";

    // Partially ordered example
    data = {3,1,2,4,8,9,7,6,5};
    // OK because:
    // - all of {3,1,2} compare less than 4
    // - all of {8,9,7,6,5} compare not less than 4
    contains = std::binary_search(data.begin(), data.end(), 4);
    // contains == true

    std::cout << "contains == " << contains << "\n";

    // Custom comparator example
    std::ranges::sort(data, std::ranges::greater{});
    // The comparator has to consistent with the ordering in the range
    contains = std::ranges::binary_search(data, 42, std::ranges::greater{});
    // contains == false

    std::cout << "contains == " << contains << "\n";

    contains = std::ranges::binary_search(data, 4, std::ranges::greater{});
    // contains == true

    std::cout << "contains == " << contains << "\n";

    // Projections are supported, however, keep in mind that when using
    // a custom projections, the required ordering applies to the projected 
    // values.
    contains = std::ranges::binary_search(data, -4, std::ranges::less{}, std::negate<>{});
    // contains == true
    // Negating the value flips the comparator from greater to less.

    std::cout << "contains == " << contains << "\n";
}
```
The function objects std::equal_to, std::not_equal_to, std::greater, std::less, std::greater_equal, std::less_equal and (C++20) std::compare_three_way implement the corresponding comparison operations.

These objects can serve as comparators for algorithms that accept them. This saves us the hassle of writing a lambda just to return the result of comparing the two arguments.
https://compiler-explorer.com/z/dsdq5dneb

```c++

struct A {};
struct B {};
bool operator<(A,B) { return false; }

int main() {
    std::vector<int> data{8,4,1,3,2,9,6,5,7};
    // Explicit specialization version:
    std::ranges::sort(data, std::greater<int>{});
    // data == {9,8,7,6,5,4,3,2,1}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // Explicit specialization can cause issues with implicit conversions.
    std::vector<double> fpoint{4.2,4.1,4.3,4.4,4.9,4.5};
    std::ranges::sort(fpoint, std::greater<int>{});
    // Unspecified order, but technically should be a no-op.
    // Reason: all elements are equal when converted to int.
    
    for (auto v : fpoint)
        std::cout << v << " ";
    std::cout << "\n";

    // C++14 <void> specialization with deduction
    std::ranges::sort(fpoint, std::greater<>{});
    // Arguments of the comparator will be deducated as double.
    // fpoint == {4.9,4.5,4.4,4.3,4.2,4.1}

    for (auto v : fpoint)
        std::cout << v << " ";
    std::cout << "\n";

    // C++20 ranges version, arguments are always deduced.
    std::ranges::sort(data, std::ranges::greater{});

    // When using with non-homogenous types, the behaviour differs.
    int x = 4; double y = 4.2;
    // Old style, explicit specified type with forced implicit conversion
    bool cmp1 = std::less<int>{}(x,y);
    bool cmp2 = std::less<double>{}(x,y);
    // cmp1 == false, cmp2 == true

    std::cout << std::boolalpha << "cmp1 == " << cmp1 << "\n" << "cmp2 == " << cmp2 << "\n";

    // C++14, both arguments are deduced, operator< potentially
    // aplied to a heterogeneous type.
    bool cmp3 = std::less<>{}(x,y); // <int,double>
    // cmp3 == true

    std::cout << "cmp3 == " << cmp3 << "\n";

    // C++20, both arguments are deduced, but constrained with stricter semantics.
    bool cmp4 = std::ranges::less{}(x,y);
    // cmp4 == true

    std::cout << "cmp4 == " << cmp4 << "\n";

    std::less<>{}(A{},B{}); // OK
    // std::ranges::less{}(A{},B{}); // Will not compile
}
```
The C++11 std::is_sorted, and std::is_sorted_until algorithms verify that the provided range is sorted in non-descending order (using the operator&lt; or a comparator).

The std::is_sorted returns a boolean; the std::is_sorted_until returns an iterator to the first out-of-order element.

Both algorithms provide parallel (C++17), constexpr (C++20) and range versions (C++20).
https://compiler-explorer.com/z/9Eq9G9EEh

```c++

int main() {
    std::vector<int> nonsort{1,2,3,4,5,2,7,8,9};
    auto r1 = std::is_sorted(nonsort.begin(), nonsort.end());
    // r1 == false

    std::cout << std::boolalpha << "r1 == " << r1 << "\n";

    auto r2 = std::is_sorted_until(nonsort.begin(), nonsort.end());
    // r2 == nonsort.begin()+5, *r2 == 2

    std::cout << "*r2 == " << *r2 << "\n";

    std::vector<std::string> sort{"x","mn","xyz","ijkl","abcde"};
    auto r3 = std::ranges::is_sorted(sort,
        [](const auto& l, const auto& r) {
            return l.length() < r.length();
        });
    // r3 == true

    // Note, that while technically the above is equivalent to
    // std::ranges::is_sorted(sort, {}, &std::string::length);
    // it would be undefined behaviour as we are not permitted to
    // take address of standard functions (including members).

    std::cout << "r3 == " << r3 << "\n";

    auto r4 = std::ranges::is_sorted_until(sort, 
        [](const auto& l, const auto& r) {
            return l.length() < r.length();
        });
    // r4 == sort.end()
    
    std::cout << "(r4 == sort.end()) == " << (r4 == sort.end()) << "\n";
}
```
Manual lifetime management and creating objects inside untyped memory blocks is a very niche topic.

However, there are situations when std::vector isn&#39;t sufficient.

Fortunately, the C++ standard library offers a set of uninitialized algorithms that provide default, copy, move and value construction and destruction on top of raw memory.
https://compiler-explorer.com/z/bqeG1oaG9

```c++

int main() {
    std::vector<std::string> src{"Hello", "World!"};
    { 
    void* buffer = std::aligned_alloc(alignof(std::string), sizeof(std::string) * src.size());
    if (buffer == nullptr) std::abort();
    auto raw_it = static_cast<std::string*>(buffer);

    { // Copy construction
    auto end_it = std::uninitialized_copy(src.begin(), src.end(), raw_it);
    // subrange(raw_it,end_it) == {"Hello", "World!"}

    for (auto &v : std::ranges::subrange(raw_it, end_it))
        std::cout << std::quoted(v) << "\n";
    
    // Manual creation requires manual destruction
    std::destroy(raw_it, end_it);
    }

    { // Copy construction from a single value
    auto end_it = raw_it + src.size();
    std::uninitialized_fill(raw_it, end_it, std::string("Something"));
    // subrange(raw_it,end_it) == {"Something", "Something"}

    for (auto &v : std::ranges::subrange(raw_it, end_it))
        std::cout << std::quoted(v) << "\n";
    
    // Manual creation requires manual destruction
    std::destroy(raw_it, end_it);
    }
    { // (C++17) Move construction
    auto end_it = raw_it + src.size();
    std::uninitialized_move(src.begin(), src.end(), raw_it);
    // subrange(raw_it,end_it) == {"Hello", "World!"}
    // src == {"", ""}

    for (auto &v : std::ranges::subrange(raw_it, end_it))
        std::cout << std::quoted(v) << "\n";
    for (auto &v : src)
        std::cout << std::quoted(v) << "\n";
    
    // Manual creation requires manual destruction
    std::destroy(raw_it, end_it);
    }

    // Free the buffer
    std::free(buffer);
    }

    { // C++20
    constexpr size_t size = 7;
    void* buffer = std::aligned_alloc(alignof(int), sizeof(int) * size);
    if (buffer == nullptr) std::abort();
    auto raw_it = static_cast<int*>(buffer);
    auto end_it = raw_it + size;

    // Value construction (for POD types this means zero-initialization)
    std::uninitialized_value_construct(raw_it, end_it);
    // subrange(raw_it, end_it) == {0, 0, 0, 0, 0, 0, 0}

    for (auto &v : std::ranges::subrange(raw_it, end_it))
        std::cout << v << " ";
    std::cout << "\n";

    // For the next example
    *raw_it = 42;

    // Manual creation requires manual destruction
    std::destroy(raw_it, end_it);

    // Default construction (for POD types this means no initialization)
    std::uninitialized_default_construct(raw_it, end_it);
    // The content is indeterminate values, however, in practical terms:
    // subrange(raw_it, end_it) == {42, 0, 0, 0, 0, 0, 0}

    for (auto &v : std::ranges::subrange(raw_it, end_it))
        std::cout << v << " ";
    std::cout << "\n";

    // Manual creation requires manual destruction
    std::destroy(raw_it, end_it);

    // Free the buffer
    std::free(buffer);
    }

}
```
The C++20 std::binary_semaphore is a specialization of the more general std::counting_semaphore that only supports two values, 0 and 1.

The main use case of a binary semaphore is for simple signalling, where the alternative approach would be to use the combination of std::mutex, std::condition_variable and a boolean variable.
https://compiler-explorer.com/z/444vWT94f

```c++

int main() {
    using namespace std::literals;
{
    std::binary_semaphore signal(0);
    auto t = std::jthread([&signal]() {
        std::osyncstream(std::cout) << std::this_thread::get_id() << " Waiting\n";

        // Wait until this thread is signaled
        signal.acquire();

        std::osyncstream(std::cout) << std::this_thread::get_id() << " Running\n";
    });

    // Injected wait to demonstrate correct ordering
    std::this_thread::sleep_for(200ms);
    std::osyncstream(std::cout) << std::this_thread::get_id() << " Before unblocking the thread.\n";
    // Signal the thread to run
    signal.release();
}
std::cout << "\n\n";
{
    // Example of how this would look with a condition variable:
    std::mutex mux;
    std::condition_variable cond;
    bool received = false;

    auto t = std::jthread([&mux, &cond, &received]() {
        std::osyncstream(std::cout) << std::this_thread::get_id() << " Waiting\n";

        // Wait until this thread is signaled
        std::unique_lock lock(mux);
        cond.wait(lock, [&received]{ return received; });

        std::osyncstream(std::cout) << std::this_thread::get_id() << " Running\n";
    });

    // Injected wait to demonstrate correct ordering
    std::this_thread::sleep_for(200ms);
    std::osyncstream(std::cout) << std::this_thread::get_id() << " Before unblocking the thread.\n";
    { // Signal the thread to run
        std::unique_lock lock(mux);
        received = true;
    }
    cond.notify_one();
}
}
```
The std::priority_queue is a container adapter implementing an ordered queue, i.e. a queue in which the largest element is always on the top and extracted first.

The behaviour is similar to manually managing a heap using heap algorithms, with a more convenient interface. However, with the caveat that the elements can only be copied from the queue, not moved out.
https://compiler-explorer.com/z/Y5nTozz6d

```c++

int main() {
    std::priority_queue<int> queue;
    // push() is a O(logn) operation
    queue.push(1);
    queue.push(2);
    queue.push(3);
    queue.push(4);

    // Prints: 4 3 2 1
    while (!queue.empty()) {
        // top() is O(1)
        std::cout << queue.top() << " ";
        // pop() is O(logn)
        queue.pop();
    }
    std::cout << "\n";

    // Example with custom comparator:
    std::priority_queue<
        // Element type
        std::string,
        // Underlying container to use
        std::vector<std::string>,
        // Custom comparator
        decltype([](const auto& left, const auto& right) {
            return left.length() > right.length();
        })> custom;

    custom.push("a");
    custom.push("aa");
    custom.push("aaa");

    // Prints "a" "aa" "aaa"
    while (!custom.empty()) {
        std::cout << std::quoted(custom.top()) << " ";
        custom.pop();
    }
    std::cout << "\n";
}
```
Before C++20, obtaining information about the source code location (line, file, function) required reliance on (sometimes non-portable) macros.

The std::source_location is a small C++20 utility that encapsulates source code location information in a C++ class. Note that the returned values are still implementation-defined.
https://compiler-explorer.com/z/M713vnW6f

```c++

void logger(std::string message,
// Tip: to capture source location at caller site, use default argument:
    std::source_location caller = std::source_location::current()) {
    std::clog << "[" << caller.file_name()  // Filename
        << ":" << caller.line()             // Line number
        << "/" << caller.column()           // Column number
        << "] " << caller.function_name()   // Function name
        << " \"" << message << "\"\n";
}

int main(int, char*[]) {
    logger("hello");
    // [/app/example.cpp:15/11] int main(int, char**) "hello"

    [](){ logger("lambda"); }();
    // [/app/example.cpp:18/17] main(int, char**)::<lambda()> "lambda"

    struct X {
        X() { logger("constructor"); }
        ~X() { logger("destructor"); }
    } var;
    // [/app/example.cpp:22/21] main(int, char**)::X::X() "constructor"
    // [/app/example.cpp:23/22] main(int, char**)::X::~X() "destructor"
}
```
The C++17 std::invoke is a utility that can invoke any callable with the provided arguments. Note that this includes member functions and even members.

If we don&#39;t need the type erasure properties of std::function or std::move_only_function, std::invoke can be a lower-level alternative (with the callable and arguments statically deduced).
https://compiler-explorer.com/z/q59nzrzr4

```c++

struct X {
    int value;
    int get_value() { return value; }
    int add(int extra) { return value + extra; }
};

int do_some_magic(int a, int b) { return a + b; }

// Customizing code with a std::(move_only_)function.
void operate(int a, int b, std::move_only_function<void(std::string)> logger) {
    int c = do_some_magic(a,b);
    logger(std::format("Did some magic with {} and {}, the result was {}.", a, b, c));
}

// Statically customizing code with a callable.
void operate_static(int a, int b, auto&& logger) {
    int c = do_some_magic(a,b);
    std::invoke(std::forward<decltype(logger)>(logger), 
        std::format("Did some magic with {} and {}, the result was {}.", a, b, c));
}

int main() {
    // Invoking typical callables (i.e. lambdas, functions...)
    auto zero_arg = [](){ return 42; };
    auto two_arg = [](int a, int b) { return a + b; };

    int a = std::invoke(zero_arg);
    // a == 42

    std::cout << "a == " << a << "\n";

    int b = std::invoke(two_arg, 98, 2);
    // b == 100

    std::cout << "b == " << b << "\n";

    // Demonstration of member invocation
    X x{42};

    int c = std::invoke(&X::value, x);
    // c == 42

    std::cout << "c == " << c << "\n";

    int d = std::invoke(&X::get_value, x);
    // d == 42

    std::cout << "d == " << d << "\n";

    int e = std::invoke(&X::add, x, 42);
    // e == 84

    std::cout << "e == " << e << "\n";

    auto logger1 = [](std::string line) { std::cout << "Logger1: " << line << "\n"; };
    auto logger2 = [](std::string line) { std::cout << "Logger2: " << line << "\n"; };

    // Two different callables, type erased:
    operate(1,2,logger1);
    operate(1,2,logger2);

    // Two different callables, two generated functions:
    operate_static(1,2,logger1);
    operate_static(1,2,logger2);
}
```
Standard C++ containers provide lexicographical comparison through the standard set of comparison operators (before C++20) and the three-way comparison operator (since C++20).

The support covers std::array, std::vector, std::deque, std::(forward_)list, std::string (and variants), std::(multi)set, std::(multi)map, std::stack and std::queue.
https://compiler-explorer.com/z/Tar5TTKna

```c++
 
int main() {
    std::vector<int> data1{1, 3, 4, 5};
    std::vector<int> data2{1, 3, 6, 7};
    std::vector<int> data3{1, 3};

    assert(data1 < data2);
    assert(data3 < data1);

    std::u8string str1 = u8"😀😄😁";
    std::u8string str2 = u8"😁😁😁";
    std::u8string str3 = u8"😁";

    assert(str1 < str2);
    assert(str3 < str2);
}
```
The std::transform_inclusive_scan and std::transform_exclusive_scan compute the inclusive/exclusive prefix sum from the results of a transformation applied to each element.

Unlike std::partial_sum, the prefix sum is generalized and not evaluated in strict order, requiring associative reduction operation for deterministic results.
https://compiler-explorer.com/z/7baj15Eve

```c++

int main() {
    std::vector<int> data{1,1,1,1,1};
    std::vector<int> out;

    std::transform_inclusive_scan(
        data.begin(), data.end(), // Input range
        std::back_inserter(out), // Output iterator
        std::plus<>{}, // Reduction operation
        std::bind_front(std::multiplies<>{},2)); // Transformation operation
    // out == {2, 4, 6, 8, 10}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";

    out.clear();
    std::transform_inclusive_scan(
        data.begin(), data.end(), // Input range
        std::back_inserter(out), // Output iterator
        std::plus<>{}, // Reduction operation
        std::bind_front(std::multiplies<>{},2), // Transformation operation
        100); // Initial value
    // out == {102, 104, 106, 108, 110}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";

    out.clear();
    std::transform_exclusive_scan(
        data.begin(), data.end(), // Input range
        std::back_inserter(out), // Output iterator
        100, // Initial value (required)
        std::plus<>{}, // Reduction operation
        std::bind_front(std::multiplies<>{},2)); // Transformation operation
    // out == {100, 102, 104, 106, 108}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";
}
```
All C++ containers directly support lexicographical comparison (three-way since C++20) of their content against another instance of the same container.

With the std::lexicographical_compare and std::lexicographical_compare_three_way (C++20), we can compare any input ranges.

std::lexicographical_compare supports both a C++17 paralel and C++20 range version.
https://compiler-explorer.com/z/dExhYj56h

```c++

int main() {
    std::vector<int> rng1{1, 2, 3, 4, 5, 6};
    std::list<int> rng2{1, 2, 3, 3, 4, 5};

    bool cmp1 = std::vector{1} < rng1;
    // cmp1 == true

    std::println("cmp1 == {}", cmp1);

    // bool cmp2 = rng1 < rng2; // Wouldn't compile
    bool cmp2 = std::lexicographical_compare(
        rng1.begin(), rng1.end(),
        rng2.begin(), rng2.end()
        // default comparator: operator<
        );
    // cmp2 == false

    std::println("cmp2 == {}", cmp2);

    // three-way comparison produces an ordering
    auto cmp3 = std::lexicographical_compare_three_way(
        rng1.begin(), std::next(rng1.begin(), 2), // {1, 2}
        rng1.begin(), std::next(rng1.begin(), 3)  // {1, 2, 3}
    );
    // decltype(cmp3) == std::strong_ordering
    // std::is_lt(cmp3) == true

    static_assert(std::is_same_v<decltype(cmp3), std::strong_ordering>);
    std::println("std::is_lt(cmp3) == {}", std::is_lt(cmp3));

    struct Wrapped { int v; };
    std::list<Wrapped> rng3{{1},{2},{3},{3},{4},{5}};
    
    auto cmp4 = std::ranges::lexicographical_compare(
        rng1, rng3,
        std::less<>{},
        std::identity{},
        &Wrapped::v);
    // cmp4 == false

    std::println("cmp4 == {}", cmp4);
}
```
Besides filesystem exploration, the std::filesystem offers the typical file manipulation operations.

Each operation offers two variants, one throwing one that returns the potential error as an outparameter.

The following example relies on throwing versions of operations to minimize error handling.
https://compiler-explorer.com/z/cn6Yde1aY

```c++

int main() {
    std::filesystem::path file = "current_file";
    {
        std::ofstream f(weakly_canonical(file));
        f << "Current content\n";
    }

    // Create a backup folder if it doesn't exist
    std::filesystem::path backup_folder = "./backup";
    if (!exists(backup_folder)) create_directory(backup_folder);

    // Check if there is sufficient space
    if (space(backup_folder).available < file_size(file))
        throw std::runtime_error("Not enough space for backup.");

    // Construct a filename with a timestamp
    std::filesystem::path backup_file = backup_folder / file.filename();
    {
        using namespace std::chrono;
        auto cnt =
            duration_cast<seconds>(system_clock::now().time_since_epoch())
                .count();
        backup_file += std::to_string(cnt);
    }
    // Create a backup
    copy(file, backup_file);

    // Create or update the symlink to the latest backup
    std::filesystem::path symlink = file.parent_path() / "current_backup";
    if (exists(symlink)) remove(symlink);
    create_symlink(backup_file, symlink);

    for (auto &v : std::filesystem::recursive_directory_iterator(".")) {
        std::cout << v << "\n";
    }
}
/* Example result:
"./current_backup" ("./backup/current_file1662980474")
"./current_file"
"./backup"
"./backup/current_file1662980474"
*/
```
The std::stable_sort is a slower version of std::sort that additionally provides stability, i.e. equivalent elements maintain their relative positions.

This is important, notably for interactive cases when one range can be repeatedly sorted based on different aspects.
https://compiler-explorer.com/z/jEsW9Gzsf

```c++

struct Data {
    int a;
    int b;
    int c;
};

int main() {
    std::vector<Data> data{
        {0, 1, 1},
        {0, 2, 1},
        {1, 1, 2},
        {1, 2, 2},
        {2, 1, 3},
        {2, 2, 3},
    };

    // Sort by b
    std::ranges::stable_sort(data, {}, &Data::b);
    for (auto [a, b, c] : data)
        std::cout << a << " " << b << " " << c << "\n";
    // Guaranteed order:
    // {{0,1,1},{1,1,2},{2,1,3},{0,2,1},{1,2,2},{2,2,3}}

    std::cout << "\n";

    // Sort by c
    std::ranges::stable_sort(data, {}, &Data::c);
    for (auto [a, b, c] : data)
        std::cout << a << " " << b << " " << c << "\n";
    // Guaranteed order:
    // {{0,1,1},{0,2,1},{1,1,2},{1,2,2},{2,1,3},{2,2,3}}
    
    std::cout << "\n";

    std::vector<std::string> labels{
       "a", "aa", "aaa", "b", "bb", "bbb", 
       "c", "cc", "ccc", "d", "dd", "ddd"
    };

    std::ranges::stable_sort(labels, {}, [](const auto& l) {
        return l.length();
    });
    for (auto &l : labels)
        std::cout << std::quoted(l) << " ";
    std::cout << "\n";
    // Guaranteed order:
    // "a", "b", "c", "d", "aa", "bb", "cc", "dd", 
    // "aaa", "bbb", "ccc", "ddd"
}
```
The std::lerp is a C++20 mathematical function that handles linear interpolation (and extrapolation) for floating-point types.

The function takes three arguments: the two boundary values and an interpolation factor. The implementation will correctly handle infinities and boundary values for the interpolation factor.
https://compiler-explorer.com/z/xdvT4434Y

```c++

int main() {
    // interpolation factor [0, 1] will interpolate
    auto mid = std::lerp(1.0, 2.0, 0.5);
    // mid ~= 1.5

    std::cout << "mid == " << mid << "\n";

	// when factor == 0, returns first argument
    auto a = std::lerp(1.0, 2.0, 0);
    // a == 1.0, guaranteed

    std::cout << "a == " << a << "\n";

    // when factor == 1, returns second argument
    auto b = std::lerp(1.0, 2.0, 1.0);
    // b == 2.0, guaranteed

    std::cout << "b == " << b << "\n";

    // extrapolation
    auto extra = std::lerp(1.0, 2.0, 1.5);
    // extra ~= 2.5

    std::cout << "extra == " << extra << "\n";

    // integral values are converted to floating point
    auto v = std::lerp(1, 2, 2);
    static_assert(std::is_same_v<decltype(v), double>);
    // v ~= 3

    std::cout << "v == " << v << "\n";
}
```
The std::shared_future is a C++11 synchronization tool suitable for one-shot single-producer/many-consumers situations.

Unlike std::future, std::shared_future is copyable, allowing multiple instances of std::shared_future to refer to the same shared state.

Similar to std::future, std::shared_future&lt;void&gt; can be used for signalling.
https://compiler-explorer.com/z/aah5fWc4v

```c++

int main() {
    using namespace std::literals;

    std::promise<int> provider;
    // Transfer the state from the provider 
    // generated future to a shared future.
    std::shared_future<int> future(provider.get_future());

    std::vector<std::jthread> runners;

    // Start a new thread, taking a copy of the future.
    runners.push_back(std::jthread([future](){
        std::osyncstream(std::cout) << std::this_thread::get_id() << " running.\n";
        int value = future.get();
        std::osyncstream(std::cout) << std::this_thread::get_id() << " thread unblocked with value " << value << "\n";
    }));

    // Start a new thread, taking a copy of the future.
    runners.push_back(std::jthread([future](){
        std::osyncstream(std::cout) << std::this_thread::get_id() << " running.\n";
        int value = future.get();
        std::osyncstream(std::cout) << std::this_thread::get_id() << " thread unblocked with value " << value << "\n";
    }));

    std::this_thread::sleep_for(200ms);

    std::osyncstream(std::cout) << std::this_thread::get_id() << " producing result.\n";
    provider.set_value(42);
}
```
Before C++20, using common mathematical constants relied on either POSIX or a compiler extension through the &lt;math.h&gt; header.

C++20 introduced a new &lt;numbers&gt; header that provides common mathematical constants as templates that can be specialized for user types. The standard library provides float, double and long double specialisations with an alias for the double variant.
https://compiler-explorer.com/z/9rd5neahn

```c++

struct Fraction {
    int numerator;
    int denominator;
};

namespace std::numbers {
    template<> inline constexpr Fraction pi_v<Fraction> = {355, 113};
}


int main() {  
    using namespace std::numbers;

    auto custom_log2 = std::log(2.4)/ln2; // ln2 == ln2_v<double>
    std::cout << "custom_log2 == " << custom_log2 << "\n";

    auto custom_log10 = std::log(2.4)/ln10; // ln10 == ln10_v<double>
    std::cout << "custom_log10 == " << custom_log10 << "\n";

    auto circle = 2*pi; // pi == pi_v<double>
    std::cout << "circle == " << circle << "\n";

    // phi == phi_v<double>
    auto approx_fibonacci = [](int in) -> int { return phi*in+0.5; };
    int fib7 = approx_fibonacci(8);  // fib7 == 13
    int fib8 = approx_fibonacci(13); // fib8 == 21
    std::cout << "fib7 == " << fib7 << ", fib8 == " << fib8 << "\n";

    // specialized constant for Fraction user-type
    auto mypi = pi_v<Fraction>;
    // mypi == {355, 133}
    std::cout << "mypi == {" << mypi.numerator << ", " << mypi.denominator << "}\n";
}
```
Despite recent developments (&lt;format&gt; and &lt;print&gt;), iostreams will be with us for the foreseeable future.

One of the quirks of iostreams is their approach to error handling, with errors represented using flags and error states.
https://compiler-explorer.com/z/eh5noa66f

```c++

int main() {
    // A stream that isn't associated with any file yet.
    std::fstream f;
    // f.good() == true, (bool)f == true
    assert(f.good() && (bool)f);

    // Trying to write into a closed stream.
    f << 10;
    // f.good() == false, f.fail() == true, f.bad() == true
    // (f.rdstate() & std::ios_base::badbit) != 0
    assert(not f.good() && f.fail() && f.bad());
    assert((f.rdstate() & std::ios_base::badbit) != 0);

    // Badbit represents an irrecoverable error,
    // i.e. the stream is in a bad state.

    // Reset the stream by opening /dev/null.
    // Reading from /dev/null immediately leads to EOF
    f.open("/dev/null");
    // f.good() == true
    assert(f.good());

    f.peek(); // Don't even read, just peek at the next character.
    // f.good() == false, f.eof() == true
    // (f.rdstate() & std::ios_base::eofbit) != 0
    assert(not f.good() && f.eof());
    assert((f.rdstate() & std::ios_base::eofbit) != 0);

    // Eofbit represents a stream that is at the end of file.

    std::stringstream s("hello");
    // Try to parse an integer from a string 
    // that contains non-digit characters.
    int v;
    s >> v;
    // s.good() == false, s.fail() == true
    // (s.rdstate() & std::ios_base::failbit) != 0
    assert(not s.good() && s.fail());
    assert((s.rdstate() & std::ios_base::failbit) != 0);

    // Failbit represents recoverable errors,
    // mostly parsing errors.

    std::string txt;
    // A stream not in a good state will not proceed
    // with further insertion/extraction.
    s >> txt;
    // txt == ""
    std::cout << "txt == " << std::quoted(txt) << "\n";

    s.clear(); // clear error bits
    s >> txt;
    // txt == "hello"
    std::cout << "txt == " << std::quoted(txt) << "\n";

    // Exceptions can be opted into, however, 
    // they do not contain useful information.
    try {
        // s is currently at eof, opting into exceptions 
        // will therefore throw immediately
        s.exceptions(std::ifstream::eofbit);
    } catch(const std::exception& e) {
        std::cout << e.what() << "\n";
    }
}
```
If we want to iterate over the elements of a bidirectional range in reverse order, we do not have to mutate it.

However, if we do want to mutate the range, we can use the std::reverse algorithm.

Note that std::list and std::forward_list provide a reverse method that reorders the nodes (instead of reversing the order of the values).
https://compiler-explorer.com/z/PEvznc99o

```c++

int main() {
    std::vector<int> data{1,2,3,4,5};
    std::reverse(data.begin(), data.end());
    // data == {5,4,3,2,1}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    std::list<int> lst1{1,2,3,4,5};
    std::forward_list<int> lst2{1,2,3,4,5};
    auto it1 = lst1.begin();
    auto it2 = lst2.begin();

    // Reverse method on list reverses nodes, instead of values
    lst1.reverse();
    // lst1 == {5,4,3,2,1}
    // it1 != lst1.begin(), *it1 == 1
    lst2.reverse();
    // lst2 == {5,4,3,2,1}
    // it2 != lst2.begin(), *it2 == 1

    std::cout << std::boolalpha;
    std::cout << "(it1 != lst1.begin()) == " << (it1 != lst1.begin()) << ", *it1 == " << *it1 << "\n";
    std::cout << "(it2 != lst2.begin()) == " << (it2 != lst2.begin()) << ", *it2 == " << *it2 << "\n";

    // Only std::list models bidirectional range
    lst1 = {1,2,3,4,5};
    it1 = lst1.begin();
    
    std::reverse(lst1.begin(), lst1.end());
    // lst1 == {5,4,3,2,1}
    // it1 == lst1.begin(), *it1 == 5

    std::cout << "(it1 == lst1.begin()) == " << (it1 == lst1.begin()) << ", *it1 == " << *it1 << "\n";
}
```
The std::queue is a container adapter that implements the interface of a FIFO queue.

The options for the backing containers are std::deque and std::list.
https://compiler-explorer.com/z/5Gbo53hMx

```c++

int main() {
    std::queue<int> q;
    // q.size() == 0, q.empty() == true
    std::cout << std::boolalpha;
    std::cout << "q.size() == " << q.size() << ", q.empty() == " << q.empty() << "\n";

    // push a new element into the queue
    q.push(1);
    // q.front() == 1, q.back() == 1
    std::cout << "q.front() == " << q.front() << ", q.back() == " << q.back() << "\n";

    // pop an element from the front of the queue
    q.pop();

    q.push(2);
    q.push(3);
    q.push(4);
    
    while(not q.empty()) {
        std::cout << q.front() << " ";
        q.pop();
        // iterate over 2, 3, 4
    }
}
```
The std::search algorithm returns the first instance of a sub-sequence.

The C++17 variant supports both parallel execution and custom searchers. Custom searchers offer better average complexity (up to linear).
https://compiler-explorer.com/z/z1K7Wfz67

```c++

int main() {
    std::string text = "the quick brown fox jumps over the lazy dog";
    std::string needle = "fox";

    // Find the first instance of a sub-sequence in text
    auto it = std::search(text.begin(), text.end(),
        needle.begin(), needle.end());
    // it != text.end()
    std::cout << std::boolalpha << (it != text.end()) << "\n";

    std::string_view word(it, it+needle.length());
    // word == "fox"
    std::cout << std::quoted(word) << "\n";

    // C++17 introduced searchers that offer better average complexity
    std::boyer_moore_horspool_searcher searcher(needle.begin(), needle.end());
    // Average linear complexity
    it = std::search(text.begin(), text.end(), searcher);
    // Same behaviour as default search
    // it != text.end()
    word = std::string_view(it, it+needle.length());
    // word == "fox"
    std::cout << std::quoted(word) << "\n";

    // Range version doesn't support searchers and returns a subrange
    auto [begin, end] = std::ranges::search(text, needle);
    word = std::string_view(begin,end);
    // word == "fox"
    std::cout << std::quoted(word) << "\n";
}
```
When implementing stream insertion and extraction, it might be convenient to expose additional options that allow the class users to control how it is formatted or parsed.

Each stream instance comes with an array of options (integers and pointers) that user-defined manipulators can use to store formatting information.
https://compiler-explorer.com/z/aW5Y5bTb7

```c++

namespace absolute {
    // Obtain a unique index which can be used to address 
    // integer or pointer value inside of all streams.
    static int flag = std::ios_base::xalloc();

    // Stream manipulators:
    struct on {
        friend std::ostream& operator<<(std::ostream& s, const on&) {
            // In the current stream, set the value of the integer
            // at index stored in flag to one.
            s.iword(flag) = 1;
            return s;
        }
        friend std::istream& operator>>(std::istream& s, const on&) {
            s.iword(flag) = 1;
            return s;
        }
    };
    struct off {
        friend std::ostream& operator<<(std::ostream& s, const off&) {
            // In the current stream, set the value of the integer
            // at index stored in flag to zero.
            s.iword(flag) = 0;
            return s;
        }
        friend std::istream& operator>>(std::istream& s, const off&) {
            s.iword(flag) = 0;
            return s;
        }
    };
}

// Object with I/O that uses the above manipulators
struct MyInt {
    int value;
    friend std::ostream& operator<<(std::ostream& s, const MyInt& v) {
        // Same as above, but only read the value
        if (s.iword(absolute::flag))
            return s << std::abs(v.value);
        else
            return s << v.value;
    }
    friend std::istream& operator>>(std::istream& s, MyInt& v) {
        // Same logic for istream
        if (s.iword(absolute::flag)) {
            if (s >> v.value)
                v.value = std::abs(v.value);
            return s;
        } else {
            return s >> v.value;
        }
    }
};

int main() {
    std::stringstream s("-9 -7 5 12");

    MyInt x,y;
    s >> absolute::on{} >> x >> absolute::off{} >> y;
    // x.value == 9, y.value == -7
    std::cout << "x == {" << x.value << "}, y == {" << y.value << "}\n";

    s >> absolute::on{} >> x >> absolute::off{} >> y;
    // x.value == 5, y.value == 12
    std::cout << "x == {" << x.value << "}, y == {" << y.value << "}\n";

    s = std::stringstream{};
    s << absolute::on{} << x << " " << absolute::off{} << y << " ";
    x.value = -9; y.value = -7;
    s << absolute::on{} << x << " " << absolute::off{} << y;
    // s.str() == "5 12 9 -7"
    std::cout << std::quoted(s.str()) << "\n";
}
```
When implementing generic code that should operate on top of ranges, the std::advance and std::distance utilities can help.

Both utilities provide constant complexity for random access ranges and fallback to linear operations.
https://compiler-explorer.com/z/KGda1sd56

```c++

int main() {  
    std::vector<int> data{1, 2, 3, 4, 5, 6, 7};
    auto it = data.begin();
    std::advance(it, 5);
    // *it == 6
    std::cout << "*it == " << *it << "\n";

    // For non-random-access ranges distance(a,b)
    // b needs to be reachable by incrementing a
    auto cnt = std::distance(data.begin(), it);
    // cnt == 5
    std::cout << "cnt == " << cnt << "\n";
    
    // For random access ranges distance(a,b)
    // either b reachable from a, or a reachable from b
    cnt = std::distance(it, data.begin()); 
    // cnt == -5
    std::cout << "cnt == " << cnt << "\n";

    // Negative distance requires a bidirectional range.
    std::advance(it, -2);
    // *it == 4
    std::cout << "*it == " << *it << "\n";
}
```
The std::string_view is a borrowed range that can reference any (not necessarily null-terminated) string, i.e. string literals, std::string and other character contiguous ranges.

std::string_view should be preferred over an immutable std::string, except for cases when ownership is also required, as std::string_view can only reference data owned by other ranges.
https://compiler-explorer.com/z/EGxvdhrcP

```c++

int main() {
    // String literals have static storage duration,
    // therefore string_view of string literal is always valid.
    std::string_view str1="The quick brown fox jumps over the lazy dog";
    std::cout << "str1 == " << std::quoted(str1) << "\n";

    // Otherwise as with any other borrowed range, the string_view 
    // should not outlive the range it is borrowing from.
    std::string_view view;
    {
        std::string owned="The quick brown fox jumps over the lazy dog";
        view = owned;
        // view is valid here
        std::cout << "view == " << std::quoted(view) << "\n";
    } // owned destroyed
    // view invalid

    // Importantly, string_views are not necessarily null terminated
    char s1[3] = {'t', 'h', 'e'};
    std::string_view str2(s1, s1+3); // OK
    // str2 == "the"
    
    std::cout << str2 << "\n"; // OK
    // std::cout << str2.data() << "\n"; // NOT OK

    std::vector<char> s2{'a','b','c'};
    // Can be constructed from any contiguous range of characters
    std::string_view str3(s2.begin(), s2.end());
    // str3 == "abc"
    std::cout << "str3 == " << std::quoted(str3) << "\n";

    // Sub-string views are easy to create
    str1.remove_prefix(4);
    // str1 == "quick brown fox jumps over the lazy dog"
    std::cout << "str1 == " << std::quoted(str1) << "\n";

    str1.remove_suffix(4);
    // str1 == "quick brown fox jumps over the lazy"
    std::cout << "str1 == " << std::quoted(str1) << "\n";

    std::string_view str4 = str1.substr(6,15);
    // str3 == "brown fox jumps"
    std::cout << "str1 == " << std::quoted(str4) << "\n";
}
```
Since C++20, the explicit specifier can be conditional. This allows for generic code that permits implicit conversions in safe cases.

In this example, the wrapper for integral types permits implicit conversion only when the destination type can represent the full range of the source type.
https://compiler-explorer.com/z/9rv5qzM7b

```c++

template<std::integral Src, std::integral Dst>
constexpr bool is_safe_conversion =
// Signed -> Signed where sizeof(Src) <= sizeof(Dst)
    (std::is_signed_v<Src> && std::is_signed_v<Dst> && 
     sizeof(Src) <= sizeof(Dst)) ||
// Unsigned -> Unsigned where sizeof(Src) <= sizeof(Dst)
    (std::is_unsigned_v<Src> && std::is_unsigned_v<Dst> && 
     sizeof(Src) <= sizeof(Dst)) ||
// Unsigned -> Signed where sizeof(Src) < sizeof(Dst)
    (std::is_unsigned_v<Src> && std::is_signed_v<Dst> && 
     sizeof(Src) < sizeof(Dst));

template <std::integral T>
struct SafeIntegral {
    constexpr SafeIntegral() : val_{} {}
    constexpr SafeIntegral(T src) : val_(src) {}

//  Either conversion operator, or conversion constructor
/*
    template<std::integral Dst>
    constexpr explicit(!is_safe_conversion<T, Dst>) 
    operator SafeIntegral<Dst>() const { return val_; }
*/
    template <std::integral Src>
    constexpr explicit(!is_safe_conversion<Src, T>)
    SafeIntegral(SafeIntegral<Src> src) : val_(src.val_) {}

    T val_;
};

int main() {
    SafeIntegral a{2}; // SafeIntegral<int>
    SafeIntegral b{2u}; // SafeIntegral<unsigned>
    // a = b; // Will not compile, unsafe conversion
    a = SafeIntegral<int>{b}; // OK explicit conversion
    
    SafeIntegral<long> c = b; // OK assuming sizeof(long) > sizeof(unsigned)
    c = a; // OK
}
```
Including the &lt;iostream&gt; header can significantly impact compile time, as it is one of the heavy standard C++ headers.

As an alternative, notably for library headers, the &lt;iosfwd&gt; header provides only forward declaration, making it essentially free.

As long as we only need to declare stream insertion and extraction functions, the &lt;iosfwd&gt; header is sufficient.
https://compiler-explorer.com/z/fTTqfs9Gx

```c++
// Sufficient for declarations

struct X {
    // Declare stream insertion
    friend std::ostream& operator<<(std::ostream&, const X&);
    // Declare stream extraction
    friend std::istream& operator>>(std::istream&, X&);
    int x;
};

// Required for definitions

// Implement stream insertion
std::ostream& operator<<(std::ostream& s, const X& x) {
    return s << x.x;
}
// Implement stream extraction
std::istream& operator>>(std::istream& s, X& x) {
    return s >> x.x;
}

int main() {
    X x;
    std::cin >> x;
    std::cout << x << "\n";
}
```
C++11 added functions for converting std::string into integer and floating point types.

Signed integers: std::stoi, std::stol, std::stoll.<br />Unsigned integers: std::stoul, std::stoull.<br />Floating point types: std::stof, std::stod, std::stold.

For std::string_view or string literals, prefer the std::from_chars function (to an excessive std::string temporary).
https://compiler-explorer.com/z/serncE6qK

```c++

int main() {
    // Normal use:
    std::string negative = "-200";
    auto v1 = stoi(negative);
    // decltype(v1) == int, v1 == -200
    static_assert(std::is_same_v<decltype(v1), int>);
    std::cout << "v1 == " << v1 << "\n";

    auto v2 = stol(negative);
    // decltype(v2) == long, v2 == -200
    static_assert(std::is_same_v<decltype(v2), long>);
    std::cout << "v2 == " << v2 << "\n";

    auto v3 = stoll(negative);
    // decltype(v3) == long long, v3 == -200
    static_assert(std::is_same_v<decltype(v3), long long>);
    std::cout << "v3 == " << v3 << "\n";

    std::string positive = "42";
    auto v4 = std::stoul(positive);
    // decltype(v4) == unsigned long, v4 == 42
    static_assert(std::is_same_v<decltype(v4), unsigned long>);
    std::cout << "v4 == " << v4 << "\n";

    auto v5 = std::stoull(positive);
    // decltype(v5) == unsigned long long, v5 == 42
    static_assert(std::is_same_v<decltype(v5), unsigned long long>);
    std::cout << "v5 == " << v5 << "\n";

    std::string pi = "3.14";
    auto v6 = std::stof(pi);
    // decltype(v6) == float, v6 ~= 3.14
    static_assert(std::is_same_v<decltype(v6), float>);
    std::cout << "v6 == " << v6 << "\n";

    auto v7 = std::stod(pi);
    // decltype(v7) == double, v7 ~= 3.14
    static_assert(std::is_same_v<decltype(v7), double>);
    std::cout << "v7 == " << v7 << "\n";

    auto v8 = std::stold(pi);
    // decltype(v8) == long double, v8 ~= 3.14
    static_assert(std::is_same_v<decltype(v8), long double>);
    std::cout << "v8 == " << v8 << "\n";

    // Optional, length of parsed text, and base for integer types:
    std::string complex = "  169AQ";
    size_t offset = 0;
    auto v9 = std::stol(complex, &offset, 10);
    // v9 == 169, decltype(v9) == long
    // std::string_view(complex.begin()+offset, complex.end()) == "AQ"
    std::cout << "v9 == " << v9 << ", " 
        << std::quoted(std::string_view(complex.begin()+offset, complex.end())) << "\n";

    offset = 0;
    auto v10 = std::stol(complex, &offset, 16);
    // v10 == 5786 (0x169A), decltype(v10) == long
    // std::string_view(complex.begin()+offset, complex.end()) == "Q"
    std::cout << "v10 == " << v10 << std::hex << " (" << v10 << "), " 
        << std::quoted(std::string_view(complex.begin()+offset, complex.end())) << "\n";

    // Note that these functions require std::string, not std::string_view.
    // For that, prefer std::from_chars.
}
```
The std::to_address is a simple C++20 utility that provides uniform access to the address from raw and smart pointers (and pointer-like objects) without the need to access the underlying value.

Before this utility was introduced, the most uniform way to obtain the address was using std::addressof(*p), which requires p to point to a valid object.
https://compiler-explorer.com/z/araWzzeTo

```c++

int main() {
    auto t1 = std::make_unique<int>(1);
    int *p1 = std::to_address(t1);
    // p1 == t1.get()
    assert(p1 == t1.get());

    std::unique_ptr<int> t2; // empty smart pointer
    int *p2 = std::to_address(t2);
    // p2 == nullptr
    assert(p2 == nullptr);

    int x = 0, *t3 = &x;
    int *p3 = std::to_address(t3);
    // t3 == p3
    assert(t3 == p3);

    // Also works for contiguous iterators
    std::vector<int> rng{1,2,3};
    int *p4 = std::to_address(rng.begin());
    // p4 == rng.data()
    assert(p4 == rng.data());
}
```
std::not_fn is a C++17 utility from the functional header that creates a simple forwarding call wrapper that returns the negation of the wrapped callable.
https://compiler-explorer.com/z/7vr4rPr1a

```c++

int main() {
    std::vector<int> data{1, 2, 3, 4, 5, 6};
    auto is_even = [](int v) { return v % 2 == 0; };

    std::vector<int> out1;
    std::copy_if(data.begin(), data.end(),
        std::back_inserter(out1), is_even);
    // out1 == {2, 4, 6}

    for (auto v : out1)
        std::cout << v << " ";
    std::cout << "\n";

    std::vector<int> out2;
    std::copy_if(data.begin(), data.end(),
        std::back_inserter(out2), std::not_fn(is_even));
    // out2 == {1, 3, 5}
    // Note: same as std::remove_copy_if with is_even

    for (auto v : out2)
        std::cout << v << " ";
    std::cout << "\n";
}
```
The function overload selection rules are complex enough to justify a book. Very roughly:

1. construct the set of viable functions (following lookup rules and deducing arguments for function templates)<br />2. the best viable function minimizes implicit conversions first and then prefers concrete functions over templates<br />3. templates are ordered by how *concrete* they are
https://compiler-explorer.com/z/xr1exf1nv

```c++

template <typename T>
void f1(T) { std::cout << "f1<>(T)\n"; }
template <>
void f1(const char*) { std::cout << "f1<>(const char*)\n"; }
void f1(int) { std::cout << "f1(int)\n"; }
void f1(std::string) { std::cout << "f1(std::string)\n"; }

template <typename T>
void f2(T, T) { std::cout << "f2<>(T,T)\n"; }
template <typename T, typename U>
void f2(T, U) { std::cout << "f2<>(T,U)\n"; }
void f2(int, double) { std::cout << "f2(int, double)\n"; }

int main() {
    using namespace std::literals;
    // 1. minimize conversions
    // 2. prefer concrete function over templates
    // 3. prefer more concrete templates over less concrete templates

    f1(0); // f1(int) and f1<>(int) are viable
           // f1(int) preferred because it is not a template

    f1(0.0); // f1(int) and f1<>(double) are viable
             // f1<>(double) preferred because it doesn't involve 
             //              an implicit conversion

    f1("hello");  // f1(std::string) and f1<>(const char*) are viable
                  // f1<>(const char*) preffered because it doesn't involve
                  //                   an implicit conversion
                  // Because there is specialization matching the template
                  // arguments, it is used instead of the base template.
                  // Note that this happens only if the base template 
                  // is the best viable candidate.

    f1("hello"s); // only f1(std::string) is viable

    f2(0,0.0); // f2(int,double), f2<>(int,double) viable
               // f2(int,double) preferred
               // Note: f2<>(T,T) can't be instantiated

    f2(0,0); // f2(int,double), f2<>(T,T), f2<>(T,U) viable
             // f2<>(T,T) and f2<>(T,U) are prefered (no conversions),
             // but f2<>(T,T) is ordered before f2<>(T,U) 
             // as it is a more specific template
}
```
C++14 introduced the [[deprecated]] attribute that can be used to mark symbols as deprecated, resulting in a warning from the compiler when the symbol is used.

Additional text can be included with the attribute and will be visible as part of the generated warning.
https://compiler-explorer.com/z/n87WrYo13

```c++
[[deprecated]]
void old_api() {}

// Optional text that will be included in the warning.
[[deprecated("Use new_api() instead.")]]
void another_old_api() {}

// Prefix:
[[deprecated]] int v; // variable
[[deprecated]] typedef int Int; // typedef
[[deprecated]] void fun() {} // function
struct Z { [[deprecated]] int z; }; // members

// Infix:
struct [[deprecated]] X {}; // struct/class/union
template <typename T> struct [[deprecated]] Y {}; // templates
template <typename T> [[deprecated]] void fn() {}
namespace [[deprecated]] Nsp {} // namespace
enum [[deprecated]] Enum {}; // enumeration

// Suffix:
using Uint [[deprecated]] = unsigned; // type alias
enum { Potato [[deprecated]] = 42 }; // enumerator


int main() {
    old_api();
    // warning: 'void old_api()' is deprecated
    another_old_api();
    // warning: 'void another_old_api()' is deprecated: Use new_api() instead.
}
```
The std::packaged_task is a C++11 callable wrapper, similar to std::function, providing direct access to a std::future.

The std::packaged_task provides a more natural and simpler workflow than manually setting up and passing through a std::promise.
https://compiler-explorer.com/z/Exsn8aMeY

```c++

int main() {
    {
    auto c = std::packaged_task{[](){
        using namespace std::literals;
        std::this_thread::sleep_for(200ms);
        return 42;
    }};

    auto future = c.get_future();
    // Start a new thread with the task:
    auto t = std::jthread(std::move(c));

    // Block until the packaged task finishes:
    int v = future.get();
    // v == 42
    std::cout << "v == " << v << "\n";
    }

    {
    // Equivalent code using a std::function and std::promise:
    std::promise<int> p;
    auto future = p.get_future();
    auto c = std::function([](std::promise<int> promise){
        using namespace std::literals;
        std::this_thread::sleep_for(200ms);
        promise.set_value(42);
    });

    // Start a new thread with the task:
    auto t = std::jthread(std::move(c), std::move(p));

    // Block until the task finishes:
    int v = future.get();
    // v == 42
    std::cout << "v == " << v << "\n";
    }
}
```
Lambdas with empty capture are still function objects but can be implicitly converted to function pointers.

In some contexts (notably when type deduction is involved), it can be helpful to force this conversion.

One approach is static_cast, but the unary plus operator offers a more concise approach.
https://compiler-explorer.com/z/hTaWdrnP8

```c++

template <typename Callback>
struct X {
    Callback call;
};

int main() {
    // Each lambda expression produces a unique distinct type.
    X x1([]() { std::cout << "Hello World!\n"; });
    X x2([]() { std::cout << "Hello Universe!\n"; });
    static_assert(!std::is_same_v<decltype(x1),decltype(x2)>);

    // unary + operator does not work on lambdas but does work on pointers
    // it forces the lambda -> function pointer implicit conversion
    X x3(+[]() { std::cout << "Hello World!\n"; });
    X x4(+[]() { std::cout << "Hello Universe!\n"; });
    static_assert(std::is_same_v<decltype(x3),decltype(x4)>);

    // static_cast approach
    X x5(static_cast<void(*)()>([]() { std::cout << "Same old.\n"; }));
    static_assert(std::is_same_v<decltype(x4), decltype(x5)>);
}
```
The std::count and std::count_if are linear search (counting) algorithms that return the number of elements matching either a provided value or a provided predicate.

Both variants support a parallel version through std::execution.
https://compiler-explorer.com/z/KTTvrvaqr

```c++

int main() {
    std::vector<int> data{1, 2, 1, 2, 3, 1, 2, 3, 4};

    // Count elements matching a value:
    auto cnt = std::count(data.begin(), data.end(), 2);
    // cnt == 3
    std::cout << "cnt == " << cnt << "\n";
    
    // Count elements matching a predicate:
    auto even = std::count_if(data.begin(), data.end(),
        [](int v) { return v % 2 == 0; });
    // even == 4
    std::cout << "even == " << even << "\n";

    // Both variants support parallel execution:
    auto even_par = std::count_if(std::execution::par_unseq,
        data.begin(), data.end(),
        [](int v) { return v % 2 == 0; });
    // even_par == 4
    std::cout << "even_par == " << even_par << "\n";
}
```
When working with objects managed by std::shared_ptr, we might need to give out access to the shared ownership of this object (e.g. when working with callbacks) without access to a std::shared_ptr that holds the ownership.

The std::enable_shared_from_this mixin provides the method shared_from_this() which returns a std::shared_ptr with access to the shared ownership.
https://compiler-explorer.com/z/39Ybehban

```c++

struct SharedObject : std::enable_shared_from_this<SharedObject> {
    std::future<void> spawn() {
        // Spawn an asynchronous lambda giving it a shared_ptr to this object.
        return std::async(std::launch::async, [handle = shared_from_this()]{
            // The std::shared_ptr will keep the instance alive,
            // at least until this lambda completes.
            using namespace std::literals;
            std::this_thread::sleep_for(200ms);
            std::osyncstream(std::cout) << "Object " << handle.get() << " still alive.\n";
        });
    }
    // Give shared ownership of this object to the caller.
    std::shared_ptr<SharedObject> give_ownership() {
        return shared_from_this();
    }
};

int main() {
    std::future<void> sync_point;
    {
    auto ptr = std::make_shared<SharedObject>();

    // Without std::enabled_shared_from_this we couldn't easily obtain
    // a handle to the instance of the object without having access
    // to the original shared_ptr.
    SharedObject& obj = *ptr;
    auto ptr2 = obj.give_ownership();
    assert(ptr.get() == ptr2.get());

    // Spawn an asynchronous operation.
    sync_point = ptr->spawn();
    }
    // At this point the original ptr handle is dead.
    std::osyncstream(std::cout) << "Original handle is dead.\n";
    // Synchronize with the async lambda.
    sync_point.wait();

    // The original object has to be held under a shared_ptr
    // calls to shared_from_this() on objects that are not
    // held by a shared_ptr will throw std::bad_weak_ptr.
    SharedObject obj;
    try {
        // This will throw
        auto handle = obj.give_ownership();
    } catch (const std::bad_weak_ptr& err) {
        std::cerr << err.what() << "\n";
    }
}
```
The C++23 added a new use case for the auto keyword. auto can now produce a prvalue copy of an argument.

auto(arg) being equivalent to T(arg) and auto{arg} being equivalent to T{arg}, with the benefit that we do not have to get access to T (using T = std::decay_t&lt;decltype(arg)&gt;;).

Due to C++17 direct initialization, auto will never produce excessive copies.
https://compiler-explorer.com/z/Gq8xEjj58

```c++

struct S {
    S() { puts("S()"); }
    S(const S&) { puts("S(const S&)"); }
    S(S&&) { puts("S(S&&)"); }
    ~S() { puts("~S()"); }
};

int main() {
    std::vector<int> data{1,2,2,1};
    std::erase(data, data[0]); // OOPS
    // data == {2,1} (implementation specific result)
    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    data = {1,2,2,1};
    std::erase(data, auto(data[0])); // correct
    // data == {2,2}
    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // direct initialization, all the way through, no copies
    auto v = auto(auto(auto(S{})));
}
```
The four algorithms: std::set_union, std::set_intersection, std::set_difference and std::set_symmetric_difference provide the corresponding set operations on top of sorted ranges.

Because C++ objects can be both equivalent yet distinct, there is additional complexity in which elements are selected.
https://compiler-explorer.com/z/axM78fz5q

```c++

struct E {
    int v;
    std::string label;
    auto operator<=>(const E& other) const {
        return v <=> other.v;
    }
};

int main() {
    std::vector<E> in1{{1,"a"},{2,"a-1"},{2,"a-2"}};
    std::vector<E> in2{{2,"b-1"},{2,"b-2"},{2,"b-3"},{3,"b"}};
    std::vector<E> out;

    // Intersection, common elements are picked from the first range
    std::set_intersection(in1.begin(), in1.end(),
        in2.begin(), in2.end(),
        std::back_inserter(out));
    // out == {{2,"a-1"},{2,"a-1"}}

    std::cout << "Intersect:\n";
    for (auto v : out)
        std::cout << v.v << " " << v.label << "\n";
    
    out.clear();
    // Union, overlapping elements are picked from the first range,
    // non-overlapping elements are picked from their source range.
    std::set_union(in1.begin(), in1.end(),
        in2.begin(), in2.end(),
        std::back_inserter(out));
    // out == {{1,"a"},{2,"a-1"},{2,"a-1"},{2,"b-3"},{3,"b"}}

    std::cout << "Union:\n";
    for (auto v : out)
        std::cout << v.v << " " << v.label << "\n";

    out.clear();
    // Difference, overlapping elements are skipped,
    // non-overlapping elements are picked from the first range.
    std::set_difference(in1.begin(), in1.end(),
        in2.begin(), in2.end(),
        std::back_inserter(out));
    // out == {{1,"a"}}

    std::cout << "Difference:\n";
    for (auto v : out)
        std::cout << v.v << " " << v.label << "\n";

    out.clear();
    // Symmetric difference, overlapping elements are skipped,
    // non-overlapping elements are picked from their source range.
    std::set_symmetric_difference(in1.begin(), in1.end(),
        in2.begin(), in2.end(),
        std::back_inserter(out));
    // out == {{1,"a"},{2,"b-3"},{3,"b"}}

    std::cout << "Symmetric:\n";
    for (auto v : out)
        std::cout << v.v << " " << v.label << "\n";
}
```
When mixing integers with floating point types in arithmetic expressions, the result of the expression is always a floating point type.

If multiple floating point types are present, the result of the expression is the highest present floating point type: float, double or long double (in that order).
https://compiler-explorer.com/z/rn5Tnbh7r

```c++

int main() {
    auto exp1 = 2 * 1.2;
    // int * double, decltype(exp1) == double
    static_assert(std::is_same_v<decltype(exp1),double>);

    int64_t x = 20;
    float y = 0.2;
    auto exp2 = x + y; // same as INT64_C(20) + .2f
    // int64_t + float, decltype(exp2) == float
    static_assert(std::is_same_v<decltype(exp2),float>);

    auto exp3 = 0.2f * 3 + 2.1;
    // float * int + double, decltype(exp3) == double
    static_assert(std::is_same_v<decltype(exp3),double>);
}
```
std::reverse_iterator is an iterator adapter that adapts (at least bidirectional) iterators for reverse iteration.

Note that to correctly model begin() and end(), the pointed-to element is offset by one.

- begin(), which points to the first element, maps to rend(), which points one before<br />- end(), which points to one after maps to rbegin(), which points to the last element
https://compiler-explorer.com/z/cx7aTv8ar

```c++

int main() {
    std::vector<int> data{1, 2, 3, 4, 5, 6};

    // bidirectional containers provide rbegin() and rend() methods
    // that return a reverse iterators
    for (auto it = data.rbegin(); it != data.rend(); it++) {
        // iterates over { 6, 5, 4, 3, 2, 1 }
        std::cout << *it << " ";
    }
    std::cout << "\n";

    // Reverse iterators are offset by one element
    auto rbegin = std::make_reverse_iterator(data.end());
    std::cout << "*rbegin == " << *rbegin << "\n";
    // end points outside of the array, rbegin points to the last element
    auto rend = std::make_reverse_iterator(data.begin());
    // begin points to the first element, rend points outside of the array
    std::cout << "*std::prev(rend) == " << *std::prev(rend) << "\n";

    // reverse iterator to the first element in original order, 
    // last element in reverse order
    auto it  = std::make_reverse_iterator(std::next(data.begin()));
    // *it == 1
    std::cout << "*it == " << *it << "\n";

    // Reverse iteration comes handy with linear algorithms
    // that operate left-to-right
    std::string text = "racecar";
    bool is_palindrome = std::equal(
            text.begin(), text.end(), // left-to-right
            text.rbegin()); // right-to-left
    // is_palindrome == true
    std::cout << "is_palindrome == " << std::boolalpha << is_palindrome << "\n";
}
```
C++23 explicit object parameter (a.k.a. deducing this) introduces the ability to name the previously implicit &quot;this&quot; argument explicitly.

This allows for different spellings of method variants (lvalue, const lvalue, rvalue).<br />Combined with type deduction, it finally allows spelling all three variants as one generic method (significantly reducing code duplication).
https://compiler-explorer.com/z/shcWdof9d

```c++

// Old spelling
struct Old {
    // Also can be spelled as: "void method() {}"
    void method() & { puts("Mutable lvalue"); }
    // Also can be spelled as: "void method() const {}"
    void method() const& { puts("Immutable lvalue"); }
    void method() && { puts("Rvalue"); }
};

// New spelling
struct New {
    void method(this New&) { puts("Mutable lvalue"); }
    void method(this const New&) { puts("Immutable lvalue"); }
    void method(this New&&) { puts("Rvalue"); }
};

// One deduced method that handles all three cases
struct Universal {
    void method(this auto&& self) {
        using T = decltype(self);
        if constexpr (std::is_lvalue_reference_v<T>) {
            if constexpr (std::is_const_v<std::remove_reference_t<T>>)
                puts("Immutable lvalue");
            else
                puts("Mutable lvalue");
        } else if constexpr (std::is_rvalue_reference_v<T>)
            puts("Rvalue");
    }
};

// A practical example of the above
struct Practical {
    // Single getter that handles all three variants
    auto&& get(this auto&& self) {
        // Based on the type of this, forward as lvalue, or rvalue
        return std::forward_like<decltype(self)>(self.value);
    }
private:
    int value = 0;
};

// We can also spell a copy
struct Object {
    // Operate on a copy of the object
    void method(this Object self) {}
};

// The deduction path can also be used as CRTP replacement
struct InjectMethod {
    // Because self is deduced, it will be the static type 
    // at the call site, i.e. the derived type.
    void do_stuff(this const auto& self) {
        puts("I did stuff.");
        self.do_other_stuff();
    }
};

struct Derived : InjectMethod {
    void do_other_stuff() const {
        puts("And then other stuff.");
    }
};

int main() {
    Old a;
    a.method();
    std::as_const(a).method();
    Old{}.method();

    New b;
    b.method();
    std::as_const(b).method();
    New{}.method();

    Universal c;
    c.method();
    std::as_const(c).method();
    Universal{}.method();

    Practical d;
    static_assert(std::is_same_v<decltype(d.get()), int&>);
    static_assert(std::is_same_v<decltype(std::as_const(d).get()), const int&>);
    static_assert(std::is_same_v<decltype(Practical{}.get()), int&&>);

    Derived e;
    e.do_stuff();
}
```
Trivially copyable types are an important performance concept in C++.

A trivially copyable object can be treated as a block of bits and copied using primitive memory operations (e.g. memcpy and memmove).
https://compiler-explorer.com/z/qzEc1v6Gc

```c++

// Trivially copyable class-types:
// - one eligible trivial copy/move-constr. or copy/move-assign.
// - no eligible non-trivial copy/move-constr. and copy/move-assign.
// - non-deleted trivial destructor

// Trivial special member function (constr., assign., destr.):
// - implicit or defaulted
// - no virtual members or bases
// - recursively trivial:
//   - all bases and non-static members have corresponding
//     trivial constr./assign./destr.

// Trivially copyable example:
// ✔ implicit copy&move constructors && copy&move assignments
// ✔ implicit destructor
// ✔ no virtual members or bases
// ✔ all members are trivially copyable
// ✔ all bases are trivially copyable
struct X {
    X() { std::iota(data.begin(), data.end(), 1); }
    std::span<int> get_buff() { return data; }
private:
    std::array<int, 42> data;
};

// Trivially copyable despite being move-only:
// ✔ defaulted move-constructor && move-assignment
// ✔ implicit destructor
// ✔ no virtual members or bases
// ✔ all members are trivially copyable
// ✔ all bases are trivially copyable
struct Y {
    Y(const Y&) = delete;
    Y& operator=(const Y&) = delete;
    Y(Y&&) = default;
    Y& operator=(Y&&) = default;
private:
    int x;
    float y;
};

// Not trivially copyable
// ❌ std::string is not trivially copyable
struct Z {
    std::string text;
};

int main() {
    static_assert(std::is_trivially_copyable_v<X>);
    static_assert(std::is_trivially_copyable_v<Y>);
    static_assert(!std::is_copy_constructible_v<Y> && 
        !std::is_copy_assignable_v<Y>);
    static_assert(!std::is_trivially_copyable_v<Z>);
}
```
C++20 introduced the &lt;bit&gt; header with a group of functions for manipulating and querying bits in unsigned integers.

These functions were previously only available as compiler extensions.
https://compiler-explorer.com/z/qEWzzcfv9

```c++

int main() {
    uint64_t x = 42u;
    // How many bits to represent a value?
    int bitcnt = std::bit_width(x);
    // bitcnt == 6
    std::cout << "bitcnt == " << bitcnt << "\n";

    // floor: largest power of two <= value
    // ceil: smallest power of two >= value
    uint64_t floor = std::bit_floor(x);
    uint64_t ceil = std::bit_ceil(x);
    // x     ==  0b101010
    // floor ==  0b100000
    // ceil  == 0b1000000
    std::cout << "    x == " << std::bitset<7>(x) << "\n";
    std::cout << "floor == " << std::bitset<7>(floor) << "\n";
    std::cout << " ceil == " << std::bitset<7>(ceil) << "\n";

    // Counting consecutive 0s/1s from left and right
    uint16_t y = 0b1110000000001111;
    int ones_left = std::countl_one(y);
    int ones_right = std::countr_one(y);
    // ones_left == 3, ones_right == 4
    std::cout << "ones_left == " << ones_left << ", ones_right == " << ones_right << "\n";

    uint16_t z = 0b0001111111110000;
    int zeros_left = std::countl_zero(z);
    int zeros_right = std::countr_zero(z);
    // zeros_left == 3, zeros_right == 4
    std::cout << "zeros_left == " << zeros_left << ", zeros_right == " << zeros_right << "\n";

    // Bitwise rotations
    uint16_t i = 0b1000100010001000;
    uint16_t a = std::rotl(i, 1);
    uint16_t b = std::rotl(i, 2);
    uint16_t c = std::rotr(i, 1);
    uint16_t d = std::rotr(i, 2);
    // a == 0b0001000100010001
    // b == 0b0010001000100010
    // c == 0b0100010001000100
    // d == 0b0010001000100010
    std::cout << "y == " << std::bitset<16>(y) << "\n";
    std::cout << "a == " << std::bitset<16>(a) << "\n";
    std::cout << "b == " << std::bitset<16>(b) << "\n";
    std::cout << "c == " << std::bitset<16>(c) << "\n";
    std::cout << "d == " << std::bitset<16>(d) << "\n";

    // Counting number of one-bits anywhere
    constexpr uint64_t FLAG_A = 1 << 0;
    constexpr uint64_t FLAG_B = 1 << 1;
    constexpr uint64_t FLAG_C = 1 << 2;

    bool exclusive = std::has_single_bit(FLAG_C);
    // exclusive == true
    int cnt = std::popcount(FLAG_A | FLAG_C);
    // cnt == 2
    std::cout << "exclusive == " << std::boolalpha << exclusive << "\n";
    std::cout << "cnt == " << cnt << "\n";
}
```
If we are writing a lambda that requires access to members of an object, we need to capture the parent object (i.e. capture &#39;this&#39;).

The options on how to achieve this have changed in C++17 and C++20, and the interactions with implicit captures [=]{} and [&amp;]{} can be non-obvious.
https://compiler-explorer.com/z/W7MWGMKdx

```c++

struct Outer {
    int x = 42; // member
    void fun() {
        int y = 7; // local variable

        [this]{ // capture this by reference
            this->x = 42; // OK
            // For the purposes of name lookup, the lambda operator() 
            // belongs to the local scope, i.e. 'this' is Outer.
            // y not captured
        }();
        [*this]{ // C++17 capture by copy
            assert(x == 42); // x is immutable
            // y is not captured
        }();
        [&]{ // implicitly capture this
            x = 42; // OK
            y = 7; // y captured by reference
        }();
        [&, this]{ // same as above, explicit spelling
            x = 42; // OK
            y = 7; //  y captured by reference
        }();
        [&, *this]{ // C++17 capture by copy
            assert(x == 42); // x is immutable
            y = 7; // y captured by reference
        }();
        [=]{ // deprecated in C++20, confusing implicit capture
            x = 42; // OK, we capture the 'this' pointer by value
                    // not the object itself
            assert(y == 7); // y is immutable
        }();
        [=, this]{ // explicit spelling remains valid
            x = 42;
            assert(y == 7); // y is immutable
        }();
        [=, *this]{ // everything by copy, C++17
            assert(x == 42); // x is immutable
            assert(y == 7); // y is immutable
        }();
    }
};

int main() {
    Outer o;
    o.fun();
}
```
The std::clamp is a simple C++17 algorithm that clamps a given value between the minimum and maximum thresholds.

If the value is outside the thresholds, the violated threshold is returned instead.
https://compiler-explorer.com/z/vWM16jjrG

```c++

int main() {
    int min = 0;
    int max = 50;

    // if the value is outside of the [min, max] range
    // the corresponding bound is returned instead
    auto x = std::clamp(-20, min, max);
    // x == 0
    std::cout << "x == " << x << "\n";

    auto y = std::clamp(70, min, max);
    // y == 50
    std::cout << "y == " << y << "\n";

    // if the value is inside the bounds (inclusive)
    // the original value is returned
    auto z = std::clamp(30, min, max);
    // z == 30
    std::cout << "z == " << z << "\n";

    // Note: std::clamp is a pass-through function.
    // When passing in temporaries, capturing by result by const-ref
    // can lead to dangling references.
    const int& w = std::clamp(1, 2, 3); // guaranteed undefined behaviour
}
```
std::istream_iterator and std::ostream_iterator are iterator adapters that can iterate over input and output streams, providing an input and output iterator respectively.

Combined with algorithms, this allows for simple streamed processing of files or data streams.
https://compiler-explorer.com/z/7d7njYr8j

```c++

struct Prompt {
  int xcoord;
  int ycoord;
  std::string label;

  // Custom input operator implementation:
  friend std::istream& operator>>(std::istream& s, Prompt& prompt) {
      s >> prompt.xcoord >> prompt.ycoord 
          >> std::quoted(prompt.label);
      return s;
  }
};

int main() {
    constexpr int min = 0;
    constexpr int max = 255;
    std::stringstream in1("-20 1 999 255 -42 42");
    std::stringstream out;
    std::transform(
        // Read integers from in1 stringstream
        std::istream_iterator<int>(in1),
        // Until EOF or parsing failure
        std::istream_iterator<int>(),
        // Output to out stringstream, delimited by one space
        std::ostream_iterator<int>(out, " "),
        // Clamp each value to the range [0..255]
        [min,max](int v) { return std::clamp(v, min, max); });
    // out1.str() == 0 1 255 255 0 42
    std::cout << out.str() << "\n";

    std::vector<Prompt> data;
    std::stringstream in2(R"(
        20 10 "Hello World!"
        -1 -5 "Maybe later?")");
    std::copy(
        // Read Prompt objects from in2 stringstream
        std::istream_iterator<Prompt>(in2),
        // Until EOF or parsing failure
        std::istream_iterator<Prompt>(),
        // Insert to vector
        std::back_inserter(data));
    // data == {{20, 10, "Hello World"}, {-1, -5, "Maybe later?"}}
    for (auto &v : data)
        std::cout << v.xcoord << " " << v.ycoord << " " << std::quoted(v.label) << "\n";
}
```
Allocators are one of the more esoteric parts of C++.

All containers rely on allocators, defaulting to std::allocator.<br />If we want to use a different allocator, we can specify it as the last template argument.

Because template arguments are part of the type, the allocator cannot be easily switched when exposing types through API boundaries.
https://compiler-explorer.com/z/ex1q7z3MK

```c++

inline size_t alloc_cnt = 0;
inline size_t dealloc_cnt = 0;

void * operator new(std::size_t n) {
    ++alloc_cnt;
    return ::malloc(n);
}
void operator delete(void * p) {
    ++dealloc_cnt;
    ::free(p);
}
void operator delete(void * p, std::size_t) {
    ++dealloc_cnt;
    ::free(p);
}

void some_func(const std::list<int>&) {}

int main() {
    // A container with an implicit allocator:
    std::list<int> list1;
    // Equivalent explicit type:
    std::list<int, std::allocator<int>> list2;
    // decltype(list1) == decltype(list2)
    static_assert(std::is_same_v<decltype(list1),decltype(list2)>);

    // Customizing the allocator:
    using BoostAlloc = boost::fast_pool_allocator<int>;
    std::list<int, BoostAlloc> list3;
    // Note that boost::pool is not very performant, I would use Intel TBB,
    // but that library is currently bugged in Compiler Explorer.
    // https://github.com/compiler-explorer/compiler-explorer/issues/5256

    // decltype(list3) != decltype(list1)
    static_assert(not std::is_same_v<decltype(list3),decltype(list1)>);
    //some_func(list3); // Wouldn't compile

    // The goal of using a pool allocator is to limit 
    // the total number of allocations.
    alloc_cnt = dealloc_cnt = 0;
    {
    std::list<int> list;
    for (size_t i = 0; i < 10'000; ++i) list.push_back(i);
    }
    // 10'000 allocations and deallocations (one for each node)
    std::cout << "Allocations " << alloc_cnt << " Deallocations " << dealloc_cnt << "\n"; 

    alloc_cnt = dealloc_cnt = 0;
    {
    std::list<int, BoostAlloc> list;
    for (size_t i = 0; i < 10'000; ++i) list.push_back(i);
    }
    // ~10 allocations
    std::cout << "Allocations " << alloc_cnt << " Deallocations " << dealloc_cnt << "\n";
}
```
std::accumulate and std::partial_sum are single-range left-fold algorithms.

Both algorithms operate strictly left-to-right, std::accumulate producing a single value, std::partial_sum emitting all partial results.

Due to the left-to-right operation, these algorithms are less performant than generalized reductions but permit reduction operations with side effects and state.
https://compiler-explorer.com/z/r13a1qE5a

```c++

void print(std::string_view label, auto &&rng);

int main() {
    std::vector<int> data{ 1, 2, 3, 4, 5, 6, 7 };

    // Both algorithms repeatedly evaluate (left-to-right):
    // 1. accumulator = accumulator + element
    // 2. accumulator = custom_op(accumulator, element)

    int res = std::accumulate(data.begin(), data.end(),
        0, // initial accumulator value, also decides accumulator type
        [](int acc, int el) {
            return acc / 2 + el;
        });
    // res == 12
    std::cout << "res == " << res << "\n";

    std::vector<int> left_fold;
    std::partial_sum(data.begin(), data.end(),
        std::back_inserter(left_fold),
        [](int acc, int el) {
            return acc / 2 + el;
        });
    // left_fold == { 1, 2, 4, 6, 8, 10, 12 }
    print("left_fold", left_fold);

    // The initial value of the accumulator for partial_sum
    // is the first element.
    // This is also the first element of the output.
    std::vector<int> single{1};
    std::vector<int> out;
    std::partial_sum(single.begin(), single.end(),
        std::back_inserter(out),
        std::plus<>{});
    // out == {1}
    print("out", out);

    // Right-fold possible for bidirectional ranges using reverse iteration:
    std::vector<int> right_fold;
    std::partial_sum(data.rbegin(), data.rend(),
        std::back_inserter(right_fold),
        [](int acc, int el) {
            return acc / 2 + el;
        });
    // right_fold == {7, 9, 9, 8, 7, 5, 3}
    print("right_fold", right_fold);
}

void print(std::string_view label, auto &&rng) {
    std::cout << label << " == {";
    std::string delim = "";
    for (auto &v : rng)
        std::cout << std::exchange(delim, ", ") << v;
    std::cout << "}\n";
}
```
Allocators allow us to control the memory allocation patterns of standard containers.

Preferably, one should use well-established allocators. However, implementing a custom allocator from scratch is not complicated.

Keep in mind that stateful allocators increase the size of each object that uses this allocator. The Monostate pattern can potentially prevent this overhead.
https://compiler-explorer.com/z/dcvT6WPrP

```c++

// Backend storage for our allocator
struct StackBuffer {
    // 512kB buffer
    alignas(alignof(std::max_align_t)) std::array<std::byte,512*1024> buffer;
    size_t used = 0;

    // Calculate the required offset for allocating type T
    // so that T is properly aligned.
    template <typename T> size_t get_offset() const {
        if (used % alignof(T) == 0) return 0;
        return alignof(T) - (used % alignof(T));
    }
    // Allocate sizeof(T)*cnt bytes in the buffer, properly aligned.
    template <typename T> T* allocate(std::size_t cnt) {
        size_t off = get_offset<T>();
        if (used + off + cnt*sizeof(T) >= buffer.size())
            throw std::bad_alloc(); // The buffer is full.

        // Pointer to the start of the allocated block.
        T* result = reinterpret_cast<T*>(buffer.data()+used+off);
        used += off + cnt*sizeof(T);
        return result;
    }
    // Deallocation is a no-op.
    void deallocate(void*, std::size_t) {}
};

// Our custom allocator
template <typename T> struct StackAllocator {
    using value_type = T; // Required

    // We need to initialize the first allocator with
    // our buffer.
    StackAllocator(StackBuffer* storage) : storage_(storage) {}
    StackAllocator(const StackAllocator&) = default;
    // Conversion constructor that passes the buffer along.
    template <typename U>
    StackAllocator(const StackAllocator<U>& other) : storage_(other.storage_) {}

    // Required
    T* allocate(std::size_t n) {
        std::cerr << "Allocating " << n << " elements of " << sizeof(T) << " bytes.\n";
        return storage_->allocate<T>(n);
    }
    // Required
    void deallocate(T* p, std::size_t n) {
        std::cerr << "Deallocating " << n << " elements of " << sizeof(T) << " bytes.\n";
        storage_->deallocate(p,n);
    }
private:
    // Required for the conversion constructor.
    template <typename U> friend struct StackAllocator;
    // Pointer to the buffer, note that this will increase
    // the size of each container that uses this allocator 
    // by sizeof(StackBuffer*), e.g. 8 bytes on x86-64.
    StackBuffer* storage_;
};

int main() {
    // String that doesn't fit into small string optimization.
    std::string_view long_string = "This is a long enough string.";

    // Create an instance of our buffer, allocating 512kB on the stack.
    StackBuffer buffer;

    // Allocator that allocates char.
    using CharAllocator = StackAllocator<char>;
    // String type that uses use the above allocator.
    using String = std::basic_string<char, std::char_traits<char>, 
                                     CharAllocator>;

    // Instead of allocating on heap, grabs 30 bytes from our buffer.
    String string_that_allocates(long_string, CharAllocator(&buffer));
    // With class template decuction, this gets more reasonable.
    std::basic_string string_ctad(long_string, CharAllocator(&buffer));
    // decltype(string_that_allocates) == decltype(string_ctad)
    static_assert(std::is_same_v<decltype(string_that_allocates),decltype(string_ctad)>);

    // Allocator that allocates the above String.
    using StringAllocator = StackAllocator<String>;

    // When we use nested containers, we would normally
    // have to manually pass in the allocator.
    std::list<String, StringAllocator> manual{StringAllocator(&buffer)};
    // Two allocations:
    // 30 bytes for the string
    // 56 bytes for the node (sizeof(std::string)+Allocator+Next+Prev)
    manual.emplace_back(long_string, CharAllocator(&buffer));
    // Note that despite passing in an allocator for String, the list
    // actually uses a converted allocator that allocates nodes.

    // However, the std::scoped_allocator_adaptor can do this for us:
    using ThroughAllocator = 
        std::scoped_allocator_adaptor<StackAllocator<String>>;

    std::list<String, ThroughAllocator> automatic{ThroughAllocator(&buffer)};
    automatic.emplace_back(long_string);
    // Note that the emplace is important here as it creates the object
    // in place, a push_back would create the string on the call site.

    // Wouldn't compile:
    // automatic.push_back(long_string);
}
```
If you require simple one-shot signalling between threads (and are stuck in pre-C++20 times), the void specializations of std::future and std::shared_future can serve as solid high-level choices for 1:1 and 1:N signalling.
https://compiler-explorer.com/z/Yq859os6G

```c++

int main() {
    using namespace std::literals;
    // Our runner with two stages.
    auto wait_for_signal = [](auto future) {
        std::osyncstream(std::cout) << std::this_thread::get_id() << " Waiting for signal.\n";
        future.wait();
        std::osyncstream(std::cout) << std::this_thread::get_id() << " Signal received.\n";
    };

    { // 1:1 example
    std::promise<void> sender;
    auto t = std::jthread(wait_for_signal, sender.get_future());
    std::this_thread::sleep_for(200ms);
    std::osyncstream(std::cout) << "Sending signal from main thread.\n";
    sender.set_value();
    }
    std::cout << "\n";

    { // 1:N example 
    std::promise<void> sender;
    // Reminder, promise::get_future() can only be called once
    std::shared_future<void> receiver(sender.get_future());

    std::vector<std::jthread> runners;
    std::generate_n(std::back_inserter(runners), 4, [&]{
        return std::jthread(wait_for_signal, receiver);
    });
    std::this_thread::sleep_for(200ms);
    std::osyncstream(std::cout) << "Sending signal from main thread.\n";
    sender.set_value();
    }
}
```
C++17 introduced the PMR (Polymorphic Memory Resource) library.

The memory resources in the library offer different allocation patterns and can be chained (a resource will use the parent resource to allocate its internal state/buffers).

The type erased std::pmr::polymorphic_allocator allows containers using different memory resources to be ABI compatible.
https://compiler-explorer.com/z/sd53rheo3

```c++

int main() {
    // The library offers shorthand aliases in the pmr namespace:
    std::pmr::list<int> list1;
    // Equivalent full type:
    std::list<int, std::pmr::polymorphic_allocator<int>> list2;
    // decltype(list1) == decltype(list2)
    static_assert(std::is_same_v<decltype(list1),decltype(list2)>);

    // The two backend resources:
    // std::pmr::new_delete_resource - calls new/delete
    // std::pmr::null_memory_resource - throws std::bad_alloc on allocation

    // OK, fits into small string optimization:
    std::pmr::string s1("hello world",
        std::pmr::null_memory_resource());
    // OK, doesn't fit into small string optimization,
    // but will call new to allocate.
    std::pmr::string s2("this string is long enough",
        std::pmr::new_delete_resource());
    try {
        // Will throw, string doesn't fit into small string optimization:
        std::pmr::string s3("this string is long enough",
            std::pmr::null_memory_resource());
    } catch (const std::bad_alloc&) {
        std::cerr << "Caught std::bad_alloc\n";
    }

    // The monotonic_buffer_resource allocates within a buffer.
    // Once the buffer is full, another buffer will be allocated
    // using the parent resource. Only deallocates on destruction.

    // The default instantiation uses the new_delete_resource 
    // and no initial buffer.
    std::pmr::monotonic_buffer_resource pool1;
    std::pmr::list<int> list3(&pool1);

    std::array<std::byte, 512*1024> buffer;
    // Example of a monotonic buffer using a stack allocated buffer
    // as the initial and only memory.
    std::pmr::monotonic_buffer_resource pool2(
        buffer.data(), buffer.size(),
        std::pmr::null_memory_resource());
    std::pmr::list<int> list4(&pool2);
    
    // The pool_resource manages pools of memory that serve
    // for allocations of the same size.
    std::pmr::unsynchronized_pool_resource pool3({
        // The pool will allocate blocks*sizeof(chunk) at a time.
        .max_blocks_per_chunk = 64, 
        // Larger requests than this will bypass the pool 
        // and be served directly by the upstream resource.
        .largest_required_pool_block = 512,
    });
    std::pmr::list<int> list5(&pool3);

    // The polymorphic_allocator correctly handles nested containers.
    std::pmr::list<std::pmr::string> list6(&pool3);
    list6.emplace_back("this string is long enough");
    // Both the node and string will be allocated in the pool.

    // The library also offers std::pmr::synchronized_pool_resource,
    // which is a thread safe version.
}
```
The std::partition_point algorithm will return the partition point, or more precisely, the end iterator of the first partition.

This will either be an iterator to the first element that doesn&#39;t satisfy the provided predicate or the end iterator of the input range if all elements do.

The input range (or the projected range) must be partitioned with respect to the predicate.
https://compiler-explorer.com/z/YeTsYWrzz

```c++

int main() {
    std::vector<int> data{9,3,5,6,8,2,4};
    // Find the first even value
    auto it = std::partition_point(data.begin(), data.end(),
        [](int v){
            return v % 2 != 0;
        });
    // *it == 6  
    std::cout << "*it == " << *it << "\n";

    // Sorted ranges are also partinioned with regards
    // to comparison against any value
    std::vector<int> sorted{1,2,3,4,5,6};
    // std::lower_bound(sorted.begin(), sorted.end(), 5) equivalent
    auto five = std::partition_point(sorted.begin(), sorted.end(),
        [](int v) { return v < 5; });
    // *five == 5
    std::cout << "*five == " << *five << "\n";

    auto not_found = std::ranges::partition_point(sorted,
        [](int v) { return v < 100; });
    // not_found == sorted.end()
    std::cout << "(not_found == sorted.end()) == " << std::boolalpha 
        << (not_found == sorted.end()) << "\n";

    // The partitioned requirement still applies when we project
    // the range using C++20 projection
    auto square = std::ranges::partition_point(sorted,
        [](int v) { return v < 16; },
        [](int v) { return v*v; }); // project to {1,4,9,16,25...}
    // *square == 4 (because 4*4 == 16)
    std::cout << "*square == " << *square << "\n";
}
```
If you work with allocators, you will want to implement allocator-aware types.

If possible, you can accept an allocator as an additional constructor argument.

If this isn&#39;t feasible, the alternative approach is to use the std::allocator_arg_t tag type as the first argument, followed by the allocator and the usual constructor arguments.
https://compiler-explorer.com/z/9e1zdv4zo

```c++

// Transparent memory resource that logs allocations
struct TracingResource final : std::pmr::memory_resource {
    TracingResource(std::string_view label, std::pmr::memory_resource* upstream) : label_(label), upstream_(upstream) {}
    std::pmr::memory_resource* upstream() const { return upstream_; }
private:
    // Allocation
    void* do_allocate(std::size_t bytes, std::size_t alignment) override {
        std::cerr << label_ << " allocating " << bytes << " bytes of memory, at " << alignment << " bytes alignment.\n";
        return upstream_->allocate(bytes,alignment);
    }
    // Deallocation
    void do_deallocate(void* p, std::size_t bytes, std::size_t alignment) override {
        std::cerr << label_ << " deallocating " << bytes << " bytes of memory, at " << alignment << " bytes alignment.\n";
        return upstream_->deallocate(p,bytes,alignment);
    }
    bool do_is_equal(const std::pmr::memory_resource& other) const noexcept override {
        auto ptr = dynamic_cast<const TracingResource*>(&other);
        if (ptr == this) return true;
        if (&other == upstream_) return true;
        return false;
    }

    std::string label_;
    std::pmr::memory_resource* upstream_;
};

// Side-note, this custom hash allows us to take a std::string_view
// as argument and use std::unordered_map methods, without
// repeated copies and conversions between std::string,
// std::pmr::string, or even pmr strings that do not use 
// the same backing resource.
// For more context see Heterogenous lookup.
struct StringHash {
  using is_transparent = void;
  [[nodiscard]] size_t operator()(const char *txt) const {
    return std::hash<std::string_view>{}(txt);
  }
  [[nodiscard]] size_t operator()(std::string_view txt) const {
    return std::hash<std::string_view>{}(txt);
  }
  [[nodiscard]] size_t operator()(const std::string &txt) const {
    return std::hash<std::string>{}(txt);
  }
  [[nodiscard]] size_t operator()(const std::pmr::string &txt) const {
    return std::hash<std::pmr::string>{}(txt);
  }
};

// An allocator aware type
struct WordCounter {
    // The demonstration uses std::pmr for brevity, however, 
    // the same approach can be applied to static allocators, 
    // with the caveat that you would need to use
    // std::scoped_allocator_adaptor.
    using allocator_type = std::pmr::polymorphic_allocator<>;

    // Default constructor, with an optional allocator argument
    WordCounter(const allocator_type& alloc = {}) : counter_(alloc) {}
    // Copy constructor
    WordCounter(const WordCounter& other, 
                const allocator_type& alloc = {}) 
      : counter_(other.counter_, alloc) {}
    // Move constructor, note that the move is conditional
    // if (alloc != other.alloc), we have to default to a copy.
    WordCounter(WordCounter&& other, 
                const allocator_type& alloc = {})
      : counter_(std::move(other.counter_), alloc) {}

    // Assignment operators remain without change
    WordCounter& operator=(const WordCounter&) = default;
    WordCounter& operator=(WordCounter&&) = default;

    // For demonstration
    void increment(std::string_view word) {
        if (auto it = counter_.find(word); it != counter_.end())
            ++(it->second);
        else
            counter_.emplace(word, 1);
    }
    void decrement(std::string_view word) {
        if (auto it = counter_.find(word);
            it != counter_.end() && it->second != 0)
            --(it->second);
    }
private:
    // Our storage that gets customized with the allocator
    std::pmr::unordered_map<
        std::pmr::string, uint64_t,
        // custom hash and std::equal_to<>
        // to allow for heterogenous lookup
        StringHash, std::equal_to<>> counter_; 
};

// If we cannot put the allocator as the last argument,
// we can use the std::allocator_arg_t tag.
template <typename... Types>
struct VariadicType {
    using allocator_type = std::pmr::polymorphic_allocator<>;

    // Tag first, followed by the allocator, other arguments follow.
    VariadicType(std::allocator_arg_t, const allocator_type& alloc, 
        Types&&... args) {}
    // And we need to keep the non-allocator version around.
    VariadicType(Types&&... args) {}

    // For copy/move, we can use the normal style
    VariadicType(const VariadicType&, const allocator_type = {}) {}
    VariadicType(VariadicType&&, const allocator_type = {});
};

int main() {
    std::pmr::monotonic_buffer_resource buffer;
    
    // We can wrap allocator aware types in containers and the outer
    // allocator will be correctly applied to the elements.
    // Note: for non-pmr, you would need std::scoped_allocator_adaptor
    TracingResource t1("WordCounter:", &buffer);
    std::pmr::vector<WordCounter> counters(&t1);

    // Construct WordCounter, using the allocator constructor.
    // The vector will allocate memory from the allocator.
    counters.emplace_back();
    // The map will allocate the bucket array and the node using the 
    // allocator. The string fits into small string optimization.
    counters[0].increment("hello");
    // Same as above, except the bucket array is already allocated
    // and the string also needs to allocate.
    counters[0].increment("this string is long enough");

    TracingResource t2("Variadic:", &buffer);
    std::pmr::vector<VariadicType<int,int,int>> variadic(&t2);
    variadic.emplace_back(1,2,3); // Calls the allocator constructor
}
```
The std::move is an unconditional xvalue cast, typically denoting that the state of the object being cast is no longer required and can be consumed during the expression evaluation.

As a cast, the std::move doesn&#39;t do any moving; the moving or consumption operation is left to the operator, constructor or function that accepts the cast object.
https://compiler-explorer.com/z/oa79rq763

```c++

struct X {
    X() { std::cout << "X()\n"; }
    X(const X&) { std::cout << "X(const X&)\n"; }
    X(X&&) { std::cout << "X(X&&)\n"; }
    ~X() { std::cout << "~X()\n"; }
};

void some_func(const X& arg) { 
    std::cout << "some_func(const X&)\n";
}
void some_func(X&& arg) { 
    std::cout << "some_func(X&&)\n";
}

// Function that accepts xvalues and prvalues
void example(X&& arg) {
    // Names of variables, functions and data members are always lvalues.
    some_func(arg); // calls some_func(const X&)

    // If the above call would call some_func(X&&), arg could 
    // be silently invalidated by that call.
    
    // However, if we actually want that, i.e. we are done with arg.
    // We can explicitly cast arg to an xvalue.
    some_func(std::move(arg)); // calls some_func(X&&)

    // After a call to a function that accepts xvalue, assume the
    // object is in moved-from state, i.e. the only valid operation
    // is assigning a new value to the object (unless the type provides
    // additional guarantees).
}

int main() {
    {
    example(X{}); // call with prvalue (temporary)
    }
    std::cout << "\n";

    std::vector<X> data;
    {   // Typical use case for std::move
        X some_var; // create a variable
        some_func(some_var); // do some operations on the variable
        // once we are done with it, we can let the final operation
        // to consume the state
        data.push_back(std::move(some_var));
    }
    data.clear();
    std::cout << "\n";

    // We can apply similar logic to getters.
    std::stack<X> stack;
    stack.push({});
    {
    auto x = stack.top(); // Copy
    auto y = std::move(stack.top()); // Move
    // However, be very careful about invalidating the internal
    // invariants of the datastructure (anything ordered).
    }
    stack.pop();
    std::cout << "\n";

    // Finally, applying the move cast to an immutable value produces
    // an immutable rvalue, and we cannot consume the state of
    // immutable values.
    {
    const X x;
    some_func(std::move(x)); // calls some_func(const X&)
    X y = std::move(x); // Copy
    // decltype(std::move(x)) == const X&&
    static_assert(std::is_same_v<decltype(std::move(x)), const X&&>);
    }
}
```
The std::inner_product is a left-fold reduction algorithm that, in each step, first reduces the current elements from both ranges into a single value and then folds the result into an accumulator.

The reduction and accumulation operations can be customized and, due to the strict left-to-right operation, are allowed to have state and side effects.
https://compiler-explorer.com/z/rM4rdvPEf

```c++

int main() {
    // Default operations are reduction: operator*, accumulate: operator+
    std::vector<int> in1{1, 2, 3, 4, 5};
    std::vector<int> in2{0, 1, 0, 1, 0};
    int res = std::inner_product(in1.begin(), in1.end(), in2.begin(), 0);
    // res == 6
    std::cout << "res == " << res << "\n";

    int custom = std::inner_product(in1.begin(), in1.end(), in2.begin(),
        1, // initial accumulator
        [](int acc, int el) { return acc * el; }, // accumulate
        [](int first, int second) { return first+second; }); // reduce
    // custom == 225
    std::cout << "custom == " << custom << "\n";

    // The two input ranges can overlap.
    // Here we use it to count the number of rising 
    // and falling edges in a signal.
    std::vector<float> signal{1.0, 0.49, 0, 0.9, 0.1};
    auto [rising, falling] = std::inner_product(
        signal.begin(), std::prev(signal.end()),
        std::next(signal.begin()),
        std::make_pair(0,0),
        [](auto acc, auto el) {
            return std::make_pair(acc.first+el.first, acc.second+el.second); 
        },
        [](float prev, float curr) {
            if (curr - prev > 0.5) return std::make_pair(1,0); // rising edge
            if (curr - prev < -0.5) return std::make_pair(0,1); // falling edge
            return std::make_pair(0,0); 
        });
    // rising == 1, falling == 2
    std::cout << "rising == " << rising << ", falling == " << falling << "\n";
}
```
Integral operands go through promotion and conversion before an operator is evaluated.

Promotion is applied to operands of lower rank than int (bool, char, short). The operands are promoted to int or unsigned (if int can&#39;t represent all values).
https://compiler-explorer.com/z/r4K3qhfeT

```c++

int main() {
    char x = 'A';
    auto exp1 = +x;
    // decltype(exp1) == int, exp1 == 65 (on ASCII platforms)
    static_assert(std::is_same_v<decltype(exp1), int>);
    std::cout << "exp1 == " << exp1 << "\n";

    uint8_t y = 12;
    auto exp2 = x + y;
    // decltype(exp2) == int, exp2 == 77 (on ASCII platforms)
    static_assert(std::is_same_v<decltype(exp2), int>);
    std::cout << "exp2 == " << exp2 << "\n";

    auto exp3 = -y;
    // decltype(exp3) == int, exp3 == -12
    static_assert(std::is_same_v<decltype(exp3), int>);
    std::cout << "exp3 == " << exp3 << "\n";

    uint8_t z = UINT8_MAX;
    auto exp4 = z * y;
    // decltype(exp4) == int, exp4 == 3060
    static_assert(std::is_same_v<decltype(exp4), int>);
    std::cout << "exp4 == " << exp4 << "\n";

    // Note: promotions make narrow types potentially problematic.
    uint16_t q = UINT16_MAX;
    auto err = q * q; // Undefined behaviour, signed overflow
    static_assert(std::is_same_v<decltype(err), int>);
}
```
After integral promotion, if the integral operands of an operator are of the same signedness but different ranks, the operand of the lower rank is converted to the type of the operand with the higher rank.
https://compiler-explorer.com/z/drcGvT9x4

```c++

void some_func(const long&) { std::cout << "const long&\n"; }
void some_func(long&) { std::cout << "long&\n"; }

int main() {
    long x = 20;
    long long y = 30;
    auto exp1 = x + y;
    // decltype(exp1) == long long, exp1 == 50
    static_assert(std::is_same_v<decltype(exp1), long long>);
    std::cout << "exp1 == " << exp1 << "\n";
    
    // Note: be careful when using unconstrained generics 
    // with built-in integral types.

    // The interactions with implicit conversion can be surprising:
    some_func(exp1); // calls some_func(const long&)
    // long long lvalue cannot bind to long&
	// long long -(impl. conv.)-> temporary long -> some_func(const long&)

    { // Potential alternatives:
    // OK, but involves a hidden implicit conversion
    long alt1 = x + y; // long long -> long
    // Wouldn't compile, deduced type long long doesn't match
    // std::same_as<long> auto alt2 = x + y;
    }

    uint8_t a = 255;
    long b = 1;
    auto exp2 = a + b;
    // decltype(exp2) == long, exp2 == 256
    // uint8_t promoted to int, then converted to long
    static_assert(std::is_same_v<decltype(exp2), long>);
    std::cout << "exp2 == " << exp2 << "\n";
}
```
Mixing the signedness of integral operands can easily lead to unexpected behaviour.

After integral promotion, if the integral operands of an operator are of different signedness, there are three possible outcomes based on the ranks and bit-width of the operands.
https://compiler-explorer.com/z/M61E76PP6

```c++

int main() {
    // [A] If the unsigned operand is of the same or higher rank
    //     => the signed operand is converted to the type 
    //        of the unsigned operand
    int a = -100;
    unsigned b = 0;
    auto exp1 = a + b;
    // decltype(exp1) == unsigned, exp1 == (-100 + (UINT_MAX + 1)) + 0
    static_assert(std::is_same_v<decltype(exp1), unsigned>);
    std::cout << "exp1 == " << exp1 << "\n";

    // [B] If the type of the signed operand can represent all values
    //     of the unsigned operand
    //     => the unsigned operand is converted to the type 
    //        of the signed operand
    long c = -100;
    unsigned d = 0;
    auto exp2 = c + d;
    // decltype(exp2) == long, exp2 == -100
    static_assert(std::is_same_v<decltype(exp2), long>);
    std::cout << "exp2 == " << exp2 << "\n";

    // [C] Otherwise
    //     => both operands are converted to unsigned version
    //        of the signed argument type
    long long e = -100;
    unsigned long f = 0; // assuming sizeof(long) == sizeof(long long)
    auto exp3 = e + f;
    // decltype(exp3) == unsigned long long
    // exp3 == (-100 + (ULLONG_MAX + 1)) + 0
    static_assert(std::is_same_v<decltype(exp3), unsigned long long>);
    std::cout << "exp3 == " << exp3 << "\n";

    // Mixing signed and unsigned types can be very error-prone:
    int x = -1;
    long y = -1;
    unsigned z = 1;

    bool first = x < z; // false, x -> unsigned, therefore x > z
    bool second = y < z; // true, z -> long, therefore y < z
    std::cout << std::boolalpha << "(x < z) == " << (x < z) << "\n";
    std::cout << std::boolalpha << "(y < z) == " << (y < z) << "\n";
}
```
C++23 introduced the stacktrace library. A stacktrace is represented as a container, with each entry representing one entry in the stackstrace.

Each entry provides a description, source file and line number.

Debug information is not required (at least for GCC).
https://compiler-explorer.com/z/416r87q8o

```c++

auto fun = []() {
    // Obtain the current stacktrace.
    return std::stacktrace::current();
};

auto caller(auto&& c) {
    return c();
}
 
int main() {
    auto trace = caller(fun);
    for (const auto& entry : trace) {
        // Description, for GCC contains the name of the callable
        std::cout << "decription: " << 
            entry.description() << "\n";
        // Source file and line number
        std::cout << "file: " << entry.source_file() 
            << " at line: " << entry.source_line() << "\n\n";
    }
    std::cout << "\n";

    // Stacktrace supports stream insertion:
    std::cout << trace << "\n";
    // And formatted output:
    std::format_to(std::ostreambuf_iterator(std::cout),
        "{}\n", trace);
}
```
std::vector is the quintessential C++ data structure.

It offers random access, low memory overhead and, due to the linear layout, is cache and branch-predictor friendly. This makes it often outperform more complex data structures.

Whenever picking a storage container, std::vector should be your first choice.
https://compiler-explorer.com/z/ch5dqbcf7

```c++

int main() {
    using namespace std::chrono;
    {
    std::cout << "std::vector\n";
    std::mt19937 rnd(0);
    std::vector<unsigned long> data; // We could also pre-allocate
    
    auto t1 = high_resolution_clock::now();
    // Fill with random data and sort.
    std::ranges::generate_n(std::back_inserter(data), 
        64*1024, [&rnd]() { return rnd(); });
    std::ranges::sort(data);

    auto t2 = high_resolution_clock::now();
    int miss = 0;
    for (auto i = 0; i < 10'000; i++) // lookup 10k elements
        if (std::ranges::lower_bound(data, rnd()) == data.end())
            ++miss;
    // Count number of misses to stop optimizer from removing this call.
    
    auto t3 = high_resolution_clock::now();
    std::cout << "Init " << duration_cast<microseconds>(t2 - t1)
        << " Runtime " << duration_cast<microseconds>(t3 - t2) 
        << " Total " << duration_cast<microseconds>(t3-t1) << "\n";

    std::cout << "Number of misses: " << miss << "\n";
    }
    std::cout << "\n";

    {
    std::cout << "std::multiset\n";
    std::mt19937 rnd(0);
    std::multiset<unsigned long> data;
    
    auto t1 = high_resolution_clock::now();
    // Fill with random (but same as above) data, no need to manually sort.
    std::ranges::generate_n(std::inserter(data, data.end()), 
        64*1024, [&rnd]() { return rnd(); });

    auto t2 = high_resolution_clock::now();
    int miss = 0;
    for (auto i = 0; i < 10'000; i++)	// lookup 10k elements
        if (data.lower_bound(rnd()) == data.end())
            ++miss;
    // Count number of misses to stop optimizer from removing this call.
    
    auto t3 = high_resolution_clock::now();
    std::cout << "Init " << duration_cast<microseconds>(t2 - t1)
        << " Runtime " << duration_cast<microseconds>(t3 - t2)
        << " Total " << duration_cast<microseconds>(t3-t1) << "\n";

    std::cout << "Number of misses: " << miss << "\n";
    }
}
```
On top of the conditional explicit specifier, C++20 gave us another tool for better control over implicit conversions.

Concepts can prevent or limit the scope of implicit conversions on the accepting side.
https://compiler-explorer.com/z/jGhv7YdYh

```c++

// Prevent implicit conversions for arguments:
void some_func1(int) {}
void some_func2(std::same_as<int> auto) {}

void demo() {
    some_func1(2.4); // OK, double -> int implicit conversion
    some_func2(2.4); // Will not compile
    // auto deduces double, std::same_as<int,double> is not satisifed
}

// Works for deduced return types:
auto some_func3() -> int {
    return 2.4 + 2; // OK
}
auto some_func4() -> std::same_as<int> auto {
    return 2.4 + 2; // Will not compile, constraint not satisfied
}

int main() {
    // As well as variables:
    uint8_t a = 1, b = 2;
    uint8_t c = a + b;
    std::same_as<uint8_t> auto d = a + b; // Will not compile
    // Integral promotion from uint8_t to int, int + int -> int
}
```
Since C++20, functions can be constrained, establishing a partial ordering of function overloads.

If multiple candidates satisfy a function invocation, the candidate with the most specific constraint will be selected.

For practical purposes, this allows for specialized implementations that take advantage of additional features provided by the arguments.
https://compiler-explorer.com/z/sbxbjx8cY

```c++

template <typename T> concept HasMethodA = requires (T t) {
  { t.method_a() } -> std::integral; };
// t.method_a() is a valid expression returning an integral type

template <typename T> concept HasMethodB = requires (T t) {
  { t.method_b() } -> std::floating_point; };
// t.method_b() is a valid expression returning a floating point type

template <typename T>
concept HasBothMethods = HasMethodA<T> && HasMethodB<T>;
// satisfies both HasMethodA and HasMethodB concepts

// For concept X = A && B, X is more specific than either A or B
// For concept X = A || B, X is less specific than either A or B

struct X {
    int method_a(){ return {}; }
};

struct Y {
    float method_b(){ return {}; }
};

struct Z {
    int method_a(){ return {}; }
    float method_b(){ return {}; }
};

// Overloads with different constraints:
void some_function(HasMethodA auto&&) { std::cout << "HasMethodA\n"; }
void some_function(HasMethodB auto&&) { std::cout << "HasMethodB\n"; }
void some_function(HasBothMethods auto&&) { std::cout << "HasBothMethods\n"; }

int main() {
    some_function(X{}); // MethodA variant, X only satisfies HasMethodA
    some_function(Y{}); // MethodB variant, Y only satisfies HasMethodB
    some_function(Z{}); // BothMethods variant
    // Z satisfies HasMethodA, HasMethodB and HasBothMethods
}
```
One way to implement custom concepts is to combine existing ones using logical AND and OR.

However, for fully custom concepts, we might need to implement custom atomic constraints using simple, type, compound and nested requirements.
https://compiler-explorer.com/z/az9seTG1T

```c++

template <std::convertible_to<std::string> Text>
struct SomeOtherType {};

template <typename T>
concept TypeRequirements = requires {
    typename T::value_type; // type requirement, T::value_type must name a type
    typename SomeOtherType<T>; // type requirements can be template instantiations
    // In this case, SomeOtherType<T> is only valid if T is convertible to std::string.
};

template <typename T>
concept SimpleAndCompound = requires (T a, T b) { // optional section for creating named arguments
    a + b; // simple requirement, expression a + b has to be valid
    { a + b }; // compound version of the same
    { a + b } noexcept; // expression valid and does not throw
    { a + b } -> std::same_as<T>; // expression valid and returns T
};

template <typename T>
concept Nested = requires {
    requires true; // simplest nested requirement, always satisfied
    requires std::convertible_to<T, int>; // nested requirement, std::convertible_to<T, int> must evaluate to true
    // Note the diference from:
    std::convertible_to<T, int>; // simple requirements, std::convertible_to<T, int> has to be a valid expression (ignores the result)
};


int main() {
}
```
C++20 added the method contains() to associative and unordered containers.

This method checks for the presence of an element, simplifying the typical pattern of invoking find() and comparing against the end iterator.
https://compiler-explorer.com/z/5dzMn6xcM

```c++

int main() {
    std::set<int> data{1, 3, 4, 5, 7};
    if (data.contains(3)) { // same as: data.find(3) != data.end()
    }
   
    // Supported for associative containers: (multi_)set, (multi_)map
    std::map<int,int> c1;
    if (c1.contains(0)) {}

    // And unordered containers: unordered_(multi)set, unordered_(multi)map
    std::unordered_set<int> c2;
    if (c2.contains(0)) {}
}
```
std::adjacent_difference is a reduction/left-fold algorithm that operates on adjacent elements of a range.

The left-fold variant permits the output range to overlap with the input range (by actively caching elements).

As with other binary algorithms, the first emitted element is simply the first element from the input range.
https://compiler-explorer.com/z/jaEjs7Ps8

```c++

int main() {
    std::vector<int> data{ 1, 2, 3, 4, 5, 6, 7 };

    // Strict left-fold operation: result = elem - old; old = move(elem);
    std::adjacent_difference(data.begin(), data.end(), data.begin());
    // data == {1, 1, 1, 1, 1, 1, 1}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // Generate Fibonacci sequence using a custom operation:
    std::adjacent_difference(data.begin(), std::prev(data.end()),
        std::next(data.begin()),
        [](int l, int r) { return l+r; });
    // data == {1, 1, 2, 3, 5, 8, 13}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // Non-linear version, output range cannot overlap input range.
    std::vector<double> average;
    std::adjacent_difference(std::execution::par_unseq,
        data.begin(), data.end(),
        std::back_inserter(average),
        [](int curr, int prev) {
            return (curr + prev) / 2.0;
        });
    // average == {1, 1, 1.5, 2.5, 4, 6.5, 10.5}

    for (auto v : average)
        std::cout << v << " ";
    std::cout << "\n";
}
```
Type erasure is a technique that allows the decoupling of the bit representation of implementation from the user.

This permits the user to be resilient against changes in the implementation and/or to operate on different implementations.

The simplest C++ approach to type erasure is through inheritance.
https://compiler-explorer.com/z/cn35dnKxr

```c++

// Abstract class, C++ doesn't have a notion of an interface:
class SomeInterface {
public:
    // Pure virtual methods that define an interface
    virtual int operation() = 0; 
    // Virtual destructor
    virtual ~SomeInterface() = default;
};

// Concrete implementations have to implement all pure methods,
// but can have any additional methods and contain arbitrary data.
struct ImplA : SomeInterface {
    ImplA(int rank) : rank(rank) {}
    int operation() override {
        return rank;
    }
    int rank;
};

struct ImplB : SomeInterface {
    ImplB(std::string text) : text(text) {}
    int operation() override {
        return std::stoi(text);
    }
    std::string text;
};

// User only depends on SomeInterface, can accept any type
// that implements SomeInterface:
void user(std::unique_ptr<SomeInterface> data) {
    int v = data->operation();
    std::cout << v << "\n";
}

int main() {
    user(std::make_unique<ImplA>(10)); // OK, prints 10
    user(std::make_unique<ImplB>(std::string{"42"})); // OK, prints 42
}
```
The inheritance approach to type erasure allows users to work simultaneously with multiple implementations.

However, if we only require one implementation at a time, we can choose a simpler and faster approach using the PIMPL pattern.
https://compiler-explorer.com/z/oz5cKf47h

```c++

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
```
One downside of relying on dynamic dispatch is the intrusive instrumentation overhead.

Types with virtual methods store a pointer to the virtual function table and are disqualified from many optimizations and conveniences (trivial copyability, aggregate initialization).

Fortunately, the same logic can be manually re-implemented externally using void* and function pointers.
https://compiler-explorer.com/z/z8YnePTof

```c++

// Concept representing the interface (optional):
template <typename T> concept Interface = requires (T t) {
  { t.operation() } -> std::same_as<int>;
};

// Owning variant of a generic holder
struct GenericHolder {
  // Only the constructor is specific to each type:
  template<Interface T>
  GenericHolder(std::unique_ptr<T> ptr) {
    // operation_ and destroy_ remember the type
    operation_ = [](void* blob) {
      return static_cast<T*>(blob)->operation(); 
    };
    destroy_ = [](void* blob) {
      delete static_cast<T*>(blob); 
    };
    blob_ = ptr.release();
  }
  ~GenericHolder() {
    if (blob_) destroy_(this->blob_);
  }

  // Move only (can be made copyable by addition of a clone_ fp)
  GenericHolder(const GenericHolder&) = delete;
  GenericHolder& operator=(const GenericHolder&) = delete;

  // Move operations
  GenericHolder(GenericHolder&& other)
      : blob_(std::exchange(other.blob_, nullptr)),
        operation_(std::exchange(other.operation_, nullptr)),
        destroy_(std::exchange(other.destroy_, nullptr)) {}
  GenericHolder& operator=(GenericHolder&& other) {
      if (blob_) destroy_(blob_);
      blob_ = std::exchange(other.blob_, nullptr);
      operation_ = std::exchange(other.operation_, nullptr);
      destroy_ = std::exchange(other.destroy_, nullptr);
      return *this;
  }

  // Actual interface
  int operation() { return operation_(this->blob_); }

private:
  // Generic storage, note that adding a new operation breaks ABI
  void *blob_;
  int (*operation_)(void*);
  void (*destroy_)(void*);
};

// Implementations are unrelated and have no virtual methods
struct ImplA {
    int operation() { return rank; }
    int rank;
};

struct ImplB {
    int operation() { return std::stoi(text); }
    std::string text;
};

void user(GenericHolder data) {
    int v = data.operation();
    std::cout << v << "\n";
}

int main() {
    user(GenericHolder(std::make_unique<ImplA>(10))); // OK, prints 10
    user(GenericHolder(std::make_unique<ImplB>("42"))); // OK, prints 42
}
```
The std::mismatch algorithm is a find-style algorithm that operates on two ranges and returns a pair of iterators to the first two elements that do not match / for which the binary predicate returns false.

With a customized comparator, the algorithm can operate as std::find with a per-element argument.
https://compiler-explorer.com/z/aKTx6rsYq

```c++

int main() {
    std::string text1 = "Welcome to the underworld!";
    std::string text2 = "Welcome to the overworld!";
    auto it = std::mismatch(text1.begin(), text1.end(),
        text2.begin());
    // *it.first == 'u', *it.second == 'o'

    std::cout << "*it.first == '" << *it.first << 
        "' *it.second == '" << *it.second << "'\n";

    std::vector<double> data = {6.0, 11.0, 2.1};
    std::vector<double> args = {1.0, 0.5, 2.0};
    // Alternative way to think about mismatch,
    // a find with per-element argument.
    auto res = std::mismatch(data.begin(), data.end(),
        args.begin(), [](double elem, double arg) {
            return elem*arg >= 5;
        });
    // *res.first == 2.1, *res.second == 2.0

    std::cout << "*res.first == '" << *res.first << 
        "' *res.second == '" << *res.second << "'\n";
}
```
The std::ranges::subrange is a convenience wrapper that can construct a range from another range or a pair of an iterator and a sentinel.

To maintain the sized property of a range, the size can be passed as an additional argument.

One use case for std::ranges::subrange is to adapt algorithms and methods that return a pair of iterators.
https://compiler-explorer.com/z/zz4soTf88

```c++

int main() {
    std::multiset<int> sorted{1,2,2,3,4,5,5,5,6,7,8,9};
    
    // multiset::equal_range() returns a pair of iterators:
    auto [left, right] = sorted.equal_range(5);

    // We can use ranges::subrange to turn that into a range:
    for (auto v : std::ranges::subrange(left, right)) {
        // Iterate over {5,5,5}
        std::cout << v << " ";
    }
    std::cout << "\n";

    // std::list is a bidirectional, sized range
    std::list<int> bidir{1,2,3,4,5};
    // OK
    std::ranges::sized_range auto t1 = bidir;
    // OK
    std::ranges::sized_range auto t2 = std::ranges::subrange(bidir);
    // OK, sized due to the provided size hint:
    std::ranges::sized_range auto t3 = 
        std::ranges::subrange(begin(bidir), end(bidir), size(bidir));
    // Wouln't compile, not sized:
    /*
    std::ranges::sized_range auto t4 = 
        std::ranges::subrange(begin(bidir), end(bidir));
    */

    // More involved example relying on ranges::subrange
    std::string_view text = R"(This is a multi-line text
Let's search for the lines)";
    auto it = text.begin();
    do {
        auto todo = std::ranges::subrange(it, text.end());
        auto line_break = std::ranges::find(todo, '\n');
        auto line = std::ranges::subrange(it, line_break);

        std::cout << std::string_view(std::begin(line), std::end(line)) << "\n";

        if (line_break == text.end()) break;
        it = std::next(line_break);
    } while (true);
    // The above is mainly for demonstration, std::views::split('\n')
    // achieves the same.
}
```
Variables with the thread_local storage specifier do not follow the regular variable lifetime. Instead, they are constructed before (typically at) their first ODR use and destroyed at thread exit.

Thread local variables do not suffer the initialization overhead of static variables. A &quot;thread local&quot; variable is only accessible from a single thread (each thread gets its copy).
https://compiler-explorer.com/z/ffMMjEvor

```c++

void task() {
    thread_local std::vector<int> data;
    data.clear(); // Drop left-over elements from previous iteration,
    // without de-allocating memory.
  
    // read-some data and do some processing
}

void runner(std::stop_token stop_token) {
  	// Check whether the associated thread requested termination.
    while (!stop_token.stop_requested()) {
      	// If not, do stuff.
        task();
    }
}

int main() {
    // Start two threads
    std::jthread t1(runner);
    std::jthread t2(runner);
    // and let them run for ~5 seconds
    using namespace std::literals::chrono_literals;
    std::this_thread::sleep_for(5s);
}
```
One potentially surprising behaviour when using the C++20 (spaceship) operator&lt;=&gt; is that unlike the defaulted version, a custom implementation will not generate equality comparison operators.

A custom operator&lt;=&gt; implies that the defaulted operator== wouldn&#39;t produce correct results, and for performance reasons, using the output from operator&lt;=&gt; isn&#39;t desirable.
https://compiler-explorer.com/z/EacnzK58K

```c++

// Equality comparable type.
// A defaulted operator<=> with no declared operator== 
// will generate a piecewise equality comparison.
struct A {
    int x;
    int y;
    auto operator<=>(const A&) const = default;
    // Same as:
    // friend auto operator<=>(const A&, const A&) = default; 
};

// Not equality comparable.
// User defined operator<=> will only generate:
// <, <=, >, >= operators
struct B {
    int x;
    int y;
    auto operator<=>(const B& other) const {
        auto cmp = x <=> other.x;
        if (!std::is_eq(cmp)) return cmp;
        return y <=> other.y;
    }
};

// Equality comparable.
// An explicitly defaulted operator== brings back
// == and != using piecewise comparison.
struct C {
    int x;
    int y;
    auto operator<=>(const C& other) const {
        auto cmp = x <=> other.x;
        if (!std::is_eq(cmp)) return cmp;
        return y <=> other.y;
    }
    bool operator==(const C&) const = default;
};

int main() {
    static_assert(std::equality_comparable<A>);
    static_assert(not std::equality_comparable<B>);
    static_assert(std::equality_comparable<C>);
}
```
The C++23 introduced the [[assume(expr)]]; attribute that can be used to introduce undefined behaviour into a program.

Instead of relying on other language-level UB, the attribute can be specified with an expression the compiler can assume is true, allowing the compiler to optimize the code aggressively.

If any assumption is violated, the program is in an invalid state.
https://compiler-explorer.com/z/bvrd3hnxa

```c++
int div2_version1(int a) {
    return a/2;
}

int div2_version2(int a) {
    // Assume positive value to allow bitshift.
    [[assume(a >= 0)]];
    return a/2;
}

unsigned case_version1(unsigned a) {
    switch (a) {
        case 0: return 1;
        case 1: return 2;
        case 2: return 3;
        default:
            return 0;
    }
}

unsigned case_version2(unsigned a) {
    switch (a) {
        case 0: return 1;
        case 1: return 2;
        case 2: return 3;
        default:
            // Mark the default case as unreachable.
            [[assume(false)]];
            return 0;
    }
}

int main() {
    // Example of triggered undefined behaviour
    case_version2(42);
}
```
The C++20 standard added another way to iterate over homogenous streams, the istream view.

The std::views::istream is the view version of std::istream_iterator, providing the expected view interface and the associated compatibility with other views.
https://compiler-explorer.com/z/cjfxYzzhT

```c++

struct Account {
    std::string name;
    int value;    
    friend std::istream& operator>>(std::istream& s, Account& p) {
        s >> std::quoted(p.name) >> p.value;
        return s;
    }
};

int main() {
    std::istringstream numbers("-10 2 4 -5 -3 9");

    // Composition with other views:
    for (auto v : std::views::istream<int>(numbers) | 
            std::views::filter([](int v) { return v >= 0; })) {
        // process non-negative integer values from numbers
        std::cout << v << " ";
    }
    std::cout << "\n";

    std::istringstream users(R"(
        "user1" 100
        "user2" 101
        "user3" 99
        "user4" 42
        "user5" 200
        "user6" 150
    )");

    // Process accounts from the input and keep the top three accounts:
    std::vector<Account> top_three(3);
    std::ranges::partial_sort_copy(
        std::views::istream<Account>(users), // until EOF or parsing fail.
        top_three,
        std::greater<>{}, // greater value
        &Account::value,  // oder by Account::value
        &Account::value); // oder by Account::value
    // top_three == {{"user5", 200}, {"user6", 150}, {"user2", 101}}

    for (auto &[label, value] : top_three)
        std::cout << label << " : " << value << "\n";
}
```
A parameter pack (C++11) is a template parameter that can represent any number of template arguments.

Without fold expressions (C++17), we have only limited options for manipulating a parameter pack.
https://compiler-explorer.com/z/vb5Gf5xYT

```c++

// Unimportant helpers
template <typename T> T transform(T&& t) { return t; }
template <typename... Ts> void other(Ts... ts) {}
struct X {};

// Passing a parameter pack around
template <typename... Ts>
void fun(Ts&&... ts) {
    // Pass to another function.
    other(ts...);
    // Same but apply a transformation to each element.
    other(transform(std::forward<Ts>(ts))...);
}

// Recursive processing
void recursive() {} // terminal state (nothing to do)

template <typename Current, typename... Ts>
void recursive(Current&& curr, Ts&&... ts) {
    std::cout << curr << " "; // process first element
    // and process the rest recursively
    recursive(std::forward<Ts>(ts)...);
}

// Non-recursive processing:
template <typename T>
void process(T&& t) { std::cout << "process(" << t << ") "; }

template <typename... Ts>
void nonrecursive(Ts&&... ts) {
    // Parameter packs can be expanded in initializer lists
    int dummy[sizeof...(Ts)] = {(process(std::forward<Ts>(ts)), 0)...};
    // (process(std::forward<Ts>(ts)), 0) is an expression that has
    // a side-effect of calling process(), but always returns zero
}

// Multiple parameter packs can be expanded in-step
template <typename... Ts> struct pack {};
template <typename A, typename B> struct pair {};

template <typename... TAs>
struct zip {
    template <typename... TBs>
    struct with {
        typedef pack<pair<TAs,TBs>...> type;
    };
};

int main() {
    fun(1, 2.4, X{});
    // other(1, 2.4, X{});
    // other(transform(1), transform(2.4), transform(X{}));

    recursive(1, 2.4, "Hello World!");
    // prints: 1, 2.4, Hello World!,
    std::cout << "\n";

    nonrecursive(2.4, 1);
    // calls: process(2.4), process(1)
    std::cout << "\n";

    typedef zip<int, double>::with<int, int>::type result_t;
    // result_t == pack<pair<int, int>, pair<double, int>>
    static_assert(std::same_as<result_t, pack<pair<int,int>, pair<double,int>>>);
}
```
Fold expressions (C++17) enable parameter pack expansions as expressions.

Fold expression can fold either left or right and allow an optional init expression.
https://compiler-explorer.com/z/jT6MEKP5K

```c++

template <int... Vs>
int minus_left() {
    return (... - Vs);
    // expands into ((Vs[0] - Vs[1]) - Vs[2]) - ... - Vs[last]
}

template <int... Vs>
int minus_right() {
    return (Vs - ...);
    // expands into Vs[0] - ... - (Vs[last-2] - (Vs[lst-1] - Vs[last]))
}

template <typename... Ts>
void print_all(Ts&&... ts) {
    // left-fold with std::cout as init
    (std::cout << ... << ts) << "\n";
    // ((std::cout << ts[0]) << ts[1]) << ...
}

template <typename... Ts>
void print_spaced(Ts&&... ts) {
    std::string_view delim = "";
    // operator, guarantees left-to-right ordering of side-effects
    // An immediately invoked lambda can wrap any "step".
    ([&]{ std::cout << std::exchange(delim," ") << ts; }(), ...);
    // expands into:
    // []{ std::cout << std::exchange(delim," ") << ts[0]; }() , 
    // ([]{ std::cout << std::exchange(delim," ") << ts[1] << " "; }(), (...))
    std::cout << "\n";
}

int main() {
    int left =  minus_left<1, 2, 3, 4, 5>();
    // (((1 - 2) - 3) - 4) - 5
    std::cout << "left == " << left << "\n";

    int right = minus_right<1, 2, 3, 4, 5>();
    // 1 - (2 - (3 - (4 - 5)))
    std::cout << "right == " << right << "\n";

    print_all(1, 2.4, "Hello World");
    // prints "12.4Hello World"

    print_spaced(1, 2.4, "Hello World");
    // prints "1 2.4 Hello World"
}
```
std::as_const is a C++17 utility that simplifies const-casting, specifically the safe variant of adding a const qualifier.

Notably, this utility is more ergonomic in generic contexts than the standard const_cast.
https://compiler-explorer.com/z/sP5MeEP6b

```c++

void test(const auto&) { std::cout << "const\n"; }
void test(auto&)  { std::cout << "mutable\n"; }

void user(auto&& x) {
    // In generic code, ensuring that we call test(const&)
    // is quite tricky to get right.
    test(const_cast<const std::remove_reference_t<decltype(x)>&>(x));
    // as_const provides a lot shorter and correct solution
    test(std::as_const(x));
}

int main() {
    int x = 0;

    // If we want to explicitly call test(const&)
    // we have to add const qualifier:
    test(const_cast<const int&>(x)); // old style
    test(std::as_const(x)); // shorter and can't mispell the type

    user(x);
    user(10);
}
```
In C++11, all containers received emplace variants of their typical insert/push methods.

The emplace variants can construct the element in place, saving a move or copy.
https://compiler-explorer.com/z/hz3c8jdMa

```c++

struct Tattler {
    Tattler() { std::cout << "Tattler()\n"; }
    Tattler(const Tattler&) { std::cout << "Tattler(const Tattler&)\n"; }
    Tattler& operator=(const Tattler&) { std::cout << "operator=(const Tattler&)\n"; return *this; }
    Tattler(Tattler&&) { std::cout << "Tattler(Tattler&&)\n"; }
    Tattler& operator=(Tattler&&) { std::cout << "operator=(Tattler&&)\n"; return *this; }
    ~Tattler() { std::cout << "~Tattler()\n"; }
};

int main() {
    std::vector<std::string> vec;
    {
        std::string s("Hello World!");
        vec.push_back(s); // Copy
        vec.push_back(std::move(s)); // Move
    }
    {
        std::string s("Hello World!");
        vec.emplace_back(s); // Copy (same as push_back)
        vec.emplace_back(std::move(s)); // Move (same as push_back)
        // In-place construction, no move or copy:
        vec.emplace_back("Hello World!");
        // Note the difference, this is still a move:
        vec.emplace_back(std::string{"Hello World!"});
    }

    std::vector<Tattler> trace;
    trace.reserve(10);
    {
        Tattler t; // Construct
        trace.push_back(t); // Copy
        trace.push_back(std::move(t)); // Move
        trace.clear(); // 2x Destruct
    } // Destruct
    std::cout << "\n";

    {
        Tattler t; // Construct
        trace.emplace_back(t); // Copy
        trace.emplace_back(std::move(t)); // Move
        trace.emplace_back(); // In-place construct
        trace.emplace_back(Tattler{}); // Construct && Move && Destruct
        trace.clear(); // 4x Destruct
    } // Destruct
}
```
The C++23 std::ranges::to is a simple utility that eagerly evaluates a view and stores the result in the specified container type.

The element type can be left out and will then be deduced using CTAD (matching the element type of the view).
https://compiler-explorer.com/z/53eYs19G4

```c++

int main() {
    std::list<int> data{1,2,3,4,5};
    auto v = data | std::ranges::to<std::vector>();
    // v == {1,2,3,4,5}
    // decltype(v) == std::vector<int>

    for (auto e : v)
        std::cout << e << " ";
    std::cout << "\n";

    // Composed views
    std::span<const char> text = "42 -13 7 3 -1 91 101";
    std::ispanstream s(text);
    auto parsed = std::views::istream<int>(s) | std::views::filter([](int e) {
        return e >= 0;
    }) | std::ranges::to<std::vector>();
    // parsed == {42, 7, 3, 91, 101}
    // decltype(parsed) == std::vector<int>

    for (auto e : parsed)
        std::cout << e << " ";
    std::cout << "\n";
 
    std::string str = "hello world";
    auto upper = str | std::views::transform([](char c) {
        return std::toupper(c);
    }) | std::ranges::to<std::string>();
    // upper == "HELLO WORLD"

    std::cout << std::quoted(upper) << "\n";

    // With an explicit element type
    std::vector<double> floats{2.4,1.1,9.7,6.3};
    auto ints = floats | std::ranges::to<std::vector<int>>();
    // ints == {2, 1, 9, 6}

    for (auto e : ints)
        std::cout << e << " ";
    std::cout << "\n";
}
```
C++20 introduced a new cast that is specifically designed for type-punning: std::bit_cast.

Unlike reinterpret_cast, std::bit_cast is permitted in constexpr contexts.
https://compiler-explorer.com/z/q9jGcxfsM

```c++

// Constexpr implementation of fast inverse root
// https://en.wikipedia.org/wiki/Fast_inverse_square_root
constexpr float fast_inverse_sqrt(float value) {
  float half = value * 0.5f;
  float y = value;

  static_assert(sizeof(float) == sizeof(uint32_t));
  uint32_t i = std::bit_cast<uint32_t>(y);
  i = 0x5f3759df - (i >> 1);
  y = std::bit_cast<float>(i);
  return y * (1.5f - (half * y * y));
}

// Standard layout type
struct X {
    int v;
};


int main() {
    constinit static float one_third = fast_inverse_sqrt(9.0);
    // one_third ~= 0.333
    std::cout << "one_third == " << one_third << "\n";

    constexpr static X x{42};
    // Wouldn't compile, can't use reinterpret cast in a constant expression
    // constinit static int y = *reinterpret_cast<const int*>(&x);

    // OK
    constinit static int y = std::bit_cast<int>(x);
    // y == 42
    std::cout << "y == " << y << "\n";

    // Assuming IEEE 754 binary64
    uint64_t val = 0;
    double zero = std::bit_cast<double>(val); // 0
    std::cout << "zero == " << zero << "\n";

    val |= UINT64_C(1) << 63;
    double negative_zero = std::bit_cast<double>(val); // -0
    std::cout << "negative_zero == " << negative_zero << "\n";

    val = 0 | (UINT64_C(0x7FF) << 52);
    double infinity = std::bit_cast<double>(val); // inf
    std::cout << "infinity == " << infinity << "\n";

    val |= UINT64_C(1) << 63;
    double negative_infinity = std::bit_cast<double>(val); // -inf
    std::cout << "negative_infinity == " << negative_infinity << "\n";
}
```
Containers of the same type can be easily compared using comparison operators.

When we need to compare the content of containers of different types, we can use the std::equal and std::is_permutation algorithms.
https://compiler-explorer.com/z/oEzr5fhvE

```c++

int main() {
    std::vector<int> data1{2, 1, 3, 4, 5};
    std::vector<int> data2{2, 4, 1, 3, 5};

    // Linear comparison:
    bool cmp1 = std::equal(data1.begin(), data1.end(), data2.begin());
    // cmp1 == false
    bool cmp2 = (data1 == data2);
    // cmp2 == false (same as std::equal if types match)

    std::cout << std::boolalpha << "cmp1 == " << cmp1 << ", cmp2 == " << cmp2 << '\n';

    // Elements match but are potentially out of order:
    bool cmp3 = std::is_permutation(data1.begin(), data1.end(), data2.begin());
    // cmp3 == true

    std::cout << std::boolalpha << "cmp3 == " << cmp3 << '\n';

    std::set<int> data3{1, 2, 3, 4, 5};

    // Linear comparison:
    bool cmp4 = std::ranges::equal(data1, data3);
    // cmp4 == false

    // Elements match but are potentially out of order:
    bool cmp5 = std::ranges::is_permutation(data1, data3);
    // cmp5 == true

    std::cout << std::boolalpha << "cmp4 == " << cmp4 << ", cmp5 == " << cmp5 << '\n';
}
```
Besides being a simple smart pointer, std::unique_ptr is also an important semantic tool, marking an ownership handoff.
https://compiler-explorer.com/z/zezzfhM1j

```c++

struct Data{};

// Function returning a unique_ptr handing off ownership to caller.
std::unique_ptr<Data> producer() { return std::make_unique<Data>(); }

// Function accepting a unique_ptr taking over ownership.
void consumer(std::unique_ptr<Data> data) {}

// Helps with Single Reponsibility Principle
// by separating resource management from logic
struct Holder {
    Holder() : data_{std::make_unique<Data>()} {}
    // implicitly defaulted move constructor && move assignment
    // implicitly deleted copy constructor && copy assignment
private:
    std::unique_ptr<Data> data_;
};

// shared_ptr has a fast constructor from unique_ptr
std::shared_ptr<Data> sptr = producer();

// Even in cases when manual resource management is required,
// a unique_ptr on the interface might be preferable:
void manual_handler(std::unique_ptr<Data> ptr) {
    Data* raw = ptr.release();
    // manual resource management
}

int main() {}
```
The std::expected (C++23) comes with a monadic interface. Relying on the monadic interface prevents the typical if-then-else verbose error checking.

The and_then and or_else methods expect a callable that accepts a value/error and returns a std::expected.

The transform and transform_error methods expect a callable that accepts a value/error and returns a value/error.
https://compiler-explorer.com/z/WWsd5Y3W3

```c++

std::expected<std::string, std::error_condition> read_input() {
    std::string s;
    if (not (std::cin >> s))
        return std::unexpected{std::make_error_condition(std::io_errc::stream)};
    return s;
}

std::expected<int, std::error_condition> to_int(const std::string& s) {
    try {
        return std::stoi(s);
    } catch (std::exception& e) {
        return std::unexpected{std::make_error_condition(std::errc::argument_out_of_domain)};
    }
}

int add_ten(int v) { return v + 10; }

std::expected<int, std::error_condition> log_error(const std::error_condition& err) {
    std::cerr << "Operation failed : " << err.message() << "\n";
    return std::unexpected{err};
}

int main() {
    auto result = read_input()
        .and_then(to_int) // invoked if the expected contains a value
        // the callable has to return a std::expected, but can change the type
        // std::expected<T,Err> -> std::expected<U,Err>
        .transform(add_ten) // invoked if the expected contains a value
        // the callable can return any type
        // std::expected<T,Err> -> std::expected<U,Err>
        .or_else(log_error); // invoked if the expected contains an error
        // the callable has to return a std::expected, but can change the type
        // std::expected<V,T> -> std::expected<V,U>
}
```
The std::next_permutation and std::prev_permutation algorithms reorder elements of a range into the next/previous lexicographical permutation.

If no such permutation exists, both algorithms roll over and return false.
https://compiler-explorer.com/z/69sfEexaE

```c++

void print(auto &&rng) {
    std::cout << "{";
    std::string delim = "";
    for (auto v : rng)
        std::cout << std::exchange(delim,",") << v;
    std::cout << "}\n";
}

int main() {
    std::vector<int> data{1, 2, 3};
    do {
        // Iterate over:
        // 123, 132, 213, 231, 312, 321
        print(data);
    } while(std::next_permutation(data.begin(), data.end()));
    // data == {1, 2, 3}
    print(data);


    std::vector<bool> bits(4);
    bits[0] = 1;
    bits[1] = 1;
    do {
        // Iterate over all 4 bit numbers with 2 bits set to 1
        // 1100, 1010, 1001, 0110, 0101, 0011
        print(bits);
    } while (std::prev_permutation(bits.begin(), bits.end()));
    // bits == {1, 1, 0, 0}
    print(bits);
}
```
Unlike some other languages, the fundamental types (bool, char, int, float...) in C++ do not receive special treatment with the following exceptions:

- fundamental types have their semantics defined in the C++ standard<br />- default initializing a variable of a fundamental type does not perform any initialization<br />- arguments to operators for fundamental types are prvalues
https://compiler-explorer.com/z/fbb6ef51v

```c++

int main() {
    int v; // left uninitialized

    // Only well-defined since C++17
    int x = 1;
    (x = 2) = x; // x == 1
    // right side evalutes: 1 (prvalue)
    // left side evaluates: ref to x (x==2)
    // assignment evaluates: ref to x (x==1)
    std::cout << "x == " << x << "\n";

    std::string y = "a";
    (y = "b") = y; // y == "b"
    // right side evaluates: ref to y
    // left side evalutes: ref y (y=="b")
    // assignment evaluates: ref to y (y=="b")
    std::cout << "y == " << y << "\n";
}
```
When copying ranges, we need to take care when the input and output ranges overlap.

For std::copy, only the tail of the destination range can overlap the source range; for std::copy_backward, only the head of the destination range can overlap the source range.
https://compiler-explorer.com/z/Pa43oKfa4

```c++

int main() {
    std::vector<int> data{ 1, 2, 3, 4, 5, 6, 7, 8, 9 };

    // OK for std::copy
    //         [ source range      ]
    // [ destination range ]
    std::copy(data.begin() + 1, data.end(), data.begin());
    // data == {2, 3, 4, 5, 6, 7, 8, 9, 9}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << '\n';

    data = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    // OK for std::copy_backward
    // [ source range      ]
    //         [ destination range ]
    std::copy_backward(data.begin(), data.begin() + 8, data.end());
    // data == {1, 1, 2, 3, 4, 5, 6, 7, 8}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << '\n';
}
```
Aggregate types can be initialized using special aggregate initialization.

This initializes members in their declaration order.

Members that are not explicitly initialized and do not have a default member initializer are initialized using empty copy-list-initialization (i.e. T x={}).
https://compiler-explorer.com/z/c8bh3sr9E

```c++

struct Data {
    int x;  
    double y;
    std::string label = "Hello World!"; // only permitted since C++14 
    std::vector<int> arr;
};

struct X {
    int a;
    int b;
};

struct Y {
    X x;
    X y;
};

int main() {
    // Initialization is done in declaration order:
    Data a = {10, 2.3};
    // a.x == 10, a.y == 2.3
    // a.label == "Hello World!", a.arr == std::vector<int>{}

    std::cout << "{.x == " << a.x << ", .y == " << a.y << ", .label == " << a.label << ", .arr == {";
    for (auto v : a.arr)
        std::cout << v << " ";
    std::cout << "}}\n";

    // Nested brackets can be omitted:
    Y b = { 10, 20, 30 };
    // b.x == {10, 20}, b.y == {30, int{} == 0}

    std::cout << "{.x == {" << b.x.a << ", " << b.x.b << "}, .y == {" << b.y.a << ", " << b.y.b << "}}\n";
}
```
C++20 introduced designated initializers for aggregate initialization.

This allows for better control over which elements of the aggregate will be explicitly initialized.
https://compiler-explorer.com/z/W14PYKqnh

```c++

struct Data {
    int a;
    double b;
    std::string c;
};

// Typical use case with default-heavy aggregate:
struct Configuration {
    enum class OptionA { Enabled, Disabled };
    OptionA option_a = OptionA::Enabled;

    std::string label = "default label";

    struct Coords { int x; int y; };
    Coords coords = { 10, 20 };
};

// A clunky but functional option for named agruments in C++
struct Arg { const std::string& label; int64_t id; };
void some_func(Arg arg) {}

int main() {
    Data x = { .b = 2.4 };
    // x == { 0, 2.4, "" }

    std::cout << "{.a == " << x.a << ", .b == " << x.b << ", .c == " << std::quoted(x.c) << "}\n";

    Configuration config = { .label = "some label" };
    // config == {OptionA::Enabled, "some label", {10, 20}};
    
    std::cout << "{.option_a == " << (config.option_a == Configuration::OptionA::Enabled ? "Enabled" : "Disabled") 
        << ", .label == " << std::quoted(config.label)
        << ", .coords == {" << config.coords.x << ", " << config.coords.y << "}}\n";

    some_func({.label = config.label, .id = 42});
}

```
The C++23 std::generator is a coroutine type for implementing generator coroutines.

The type implements a view interface and comes with a std::pmr::generator alias for use with the PMR allocator.

The capability of yielding ranges of values makes the std::generator particularly useful for recursive traversals.
https://compiler-explorer.com/z/Eqdn6Yqss

```c++

// Fibonacci number generator
std::generator<int64_t> fibonacci(int64_t cnt) {
    int64_t first = 0;
    int64_t second = 1;
    while (cnt > 0) {
        co_yield first;
        first = std::exchange(second, first + second);
        --cnt;
    }
}

struct Tree {
    struct Node {
        int64_t value;
        Node* left;
        Node* right;
    };
    Node* root;
    std::vector<std::unique_ptr<Node>> store_;
};

Tree make_tree();

// In-Order tree traversal implemented using std::generator.
std::generator<Tree::Node*> in_order(Tree::Node* root) {
    if (root == nullptr)
        co_return;
    if (root->left != nullptr)
        co_yield std::ranges::elements_of(in_order(root->left));
    co_yield root;
    if (root->right != nullptr)
        co_yield std::ranges::elements_of(in_order(root->right));
}

int main() {
    // First 10 fibonacci numbers
    for (int64_t v : fibonacci(10))
        std::cout << v << " ";
    std::cout << "\n";

    Tree tree = make_tree();
    // Traverse the tree using in_order traversal
    for (Tree::Node *node : in_order(tree.root))
        std::cout << node->value << " ";
    std::cout << "\n";
}

Tree make_tree() {
    Tree tree;
    auto push = [&](int64_t value) {
        tree.store_.push_back(std::make_unique<Tree::Node>(value, nullptr, nullptr));
        return tree.store_.back().get();
    };
    tree.root = push(5);
    tree.root->left = push(3);
    tree.root->right = push(7);
    tree.root->left->left = push(1);
    tree.root->left->right = push(4);
    tree.root->right->left = push(6);
    tree.root->right->right = push(8);
    return tree;
}
```
C++20 added prefix and suffix checking methods: starts_with and ends_with to both std::string and std::string_view.
https://compiler-explorer.com/z/PGTbq9xos

```c++

int main() {
    std::string str("the quick brown fox jumps over the lazy dog");
    bool t1 = str.starts_with("the quick"); // const char* overload
    // t1 == true
    bool t2 = str.ends_with('g'); // char overload
    // t2 == true

    std::cout << std::boolalpha << "t1 == " << t1 << "\n";
    std::cout << std::boolalpha << "t2 == " << t2 << "\n";

    std::string_view needle = "lazy dog";
    bool t3 = str.ends_with(needle); // string_view overload
    // t3 == true

    std::cout << std::boolalpha << "t3 == " << t3 << "\n";

    std::string_view haystack = "you are a lazy cat";
    // both starts_with and ends_with also available for string_view
    bool t4 = haystack.ends_with(needle);
    // t4 == false

    std::cout << std::boolalpha << "t4 == " << t4 << "\n";
}
```
The ordered containers std::(multi_)set and std::(multi_)map are node-based containers that offer log(n) operation complexity for lookup, insertion and removal.

As with other node-based containers, we pay for the reference and iterator stability with performance.

Due to a relatively low constant overhead, ordered containers can sometimes outperform unordered containers.
https://compiler-explorer.com/z/na93K6dse

```c++

struct Blob { 
    int a;
    int b;
};

struct Key {
    uint64_t id;
    std::string label;
    auto operator<=>(const Key&) const = default;
};

int main() {
    std::map<uint64_t,Blob> data;

    // insert new element if key doesn't exist
    data.insert(std::make_pair(0z, Blob{10,20}));
    // insert if key doesn't exist, update value if key already exists
    data.insert_or_assign(1z, Blob{1, 2});
    // if key doesn't exist, insert a new element, constructing
    // the value in-place from the arguments
    data.try_emplace(1z, 1, 2); // 1, 2 used for the value

    auto it1 = data.find(0); // lookup by key
    // it1->first == 0, it1->second == {10, 20}
    auto it2 = data.find(4);
    // it2 == data.end()

    std::cout << it1->first << " : {" << it1->second.a << "," << it1->second.b << "}\n";
    std::cout << std::boolalpha << "(it2 == data.end()) == " << (it2 == data.end()) << "\n\n";

    // iterate over elements in strict-weak ordering
    for (auto& [key, value] : data)
        std::cout << key << " : {" << value.a << "," << value.b << "}\n";
    std::cout << "\n";

    // The key type has to support strict-weak ordering
    std::set<Key> set{{0, "label1"}, {0, "label2"},
                      {1, "label1"}, {1, "label2"}};

    bool check = set.contains({0, "label1"});
    // check == true

    std::cout << std::boolalpha << "check == " << check << "\n";
}
```
When relying on designated initializers during aggregate initialization, it might be desirable to ensure that some fields are always explicitly initialized.

We can use a simple wrapper that cannot be constructed from an empty initializer list to enforce initialization.
https://compiler-explorer.com/z/ozPMEr6x8

```c++

template <typename T>
struct RequiredField {
    RequiredField(const T& value) : value(value) {}
    operator T&() { return value; }
    operator const T&() const { return value; }
    T value;
};

struct Options {
    RequiredField<int> id;
    std::string label = "default label";
};

int main() {
    Options options = { .id = 20 };
    // options.id == 20, options.label == "default label"

    std::cout << ".id == " << options.id << ", .label == " << std::quoted(options.label) << "\n";

    // Options opt = {.label = "some label"}; // Won't compile.
    // RequiredField is not constructible from empty initializer list.
}
```
The std::string is a container for storing null-terminated narrow character strings.

std::string provides very similar functionality to std::vector while maintaining the null-termination invariant on every operation.

Additionally, std::string also provides a couple of convenience methods (find, starts/ends_with, contains, substr).
https://compiler-explorer.com/z/M5e4dceWf

```c++

int main() {
    std::string str = "Hello World?";
    
    // Same interface as std::vector
    str.pop_back();
    str.push_back('!');
    // str == "Hello World!"

    std::cout << "str == " << std::quoted(str) << "\n";

    // Importantly, these operations maintain null-termination
    char c = str[str.size()]; // Unlike std::vector, this is OK
    // c == char{}, i.e. '\0'

    std::cout << "c == " << int(c) << "\n";
    
    // Convenience methods
    size_t pos = str.find_first_of(" ?");
    // pos == 5

    std::cout << "pos == " << pos << "\n";

    // Get a C-style string
    const char* cstring = str.c_str();
    // cstring == "Hello World!"

    std::cout << "cstring == " << std::quoted(cstring) << "\n";

    // Substring, offset and count
    auto world = str.substr(6, 5);
    // world == "World"

    std::cout << "world == " << std::quoted(world) << "\n";

    // Count can be omitted to take the remainder of the string
    auto WORLD = str.substr(6);
    // WORLD == "World!"

    std::cout << "WORLD == " << std::quoted(WORLD) << "\n";
}
```
The C++23 std::print and std::println are the counterparts to std::format that format to a stdio FILE descriptor instead of producing a std::string.

Both std::print and std::println by default output to the standard output (stdout).
https://compiler-explorer.com/z/Y9dvx6xr3

```c++

int main() {
    // Outputs through stdio to stdout
    std::print("{} {}!\n", "Hello", "World");
    // Same as above
    std::println("{} {}!", "Hello", "World");

    // The FILE descriptor can be specified as the first argument
    std::FILE *f = std::fopen("out.txt", "w");
    // Output will be written to "out.txt"
    std::println(f, "{} {}!", "Hello", "World");
    std::fclose(f);

    // Standard error output
    std::println(stderr, "{} {}!", "Hello", "World");
    
    // Same formatting options as std::format
    std::println("The first five multiples of 17 : {:n}", 
        std::views::iota(0,5) | 
        std::views::transform([](int v) { return v * 17; }));
    // prints:
    // The first ten multiples of 17 : 0, 17, 34, 51, 68
}
```
C++17 introduced inline variables.

Inline variables can appear in headers as they are permitted multiple definitions (as long as there is only one per unit and all definitions are the same).

One of the notable use cases of inline variables is for customization function objects (Niebloids) since non-function symbols inhibit ADL lookup.
https://compiler-explorer.com/z/3PKE3scrs

```c++

namespace SomeNamespace {
    struct X {};
    // A typical use case, non-function symbols do not participate in ADL.
    constexpr inline auto fun = [](auto&&) { std::cout << "SomeNamespace::fun()\n"; };
    // constexpr not required, but advisable
}

namespace OtherNamespace {
    struct Y {};
    void fun(auto&&) { std::cout << "OtherNamespace::fun()\n"; };
}

int main() {
    SomeNamespace::X x;
    // fun(x); // Would not compile, does not participate in ADL.
    SomeNamespace::fun(x); // OK

    using SomeNamespace::fun;
    OtherNamespace::Y y;
    fun(y); // calls SomeNamespace::fun
    // OtherNamespace::fun is only visible to ADL
}
```
std::reduce (C++17) is a generalized reduction algorithm, a counterpart to the left-fold std::accumulate.

std::reduce can only operate with an associative and commutative operation as the reduction is not evaluated in strict left-to-right order.

However, consecutively, the algorithm also supports parallel execution through std::execution.
https://compiler-explorer.com/z/qa7v93sWz

```c++

int main() {
    std::vector<int> data{1, 2, 3, 4, 5, 6, 7};

    // Basic reduce with int{} (0) as starting value 
    // and std::plus<int>{} as the reduction operation
    int sum = std::reduce(data.begin(), data.end());
    // sum == 28

    std::cout << "sum == " << sum << "\n";

    // Reduce with custom init value and reduction operation
    int product = std::reduce(data.begin(), data.end(),
                            1, std::multiplies<>{});
    // product == 5040

    std::cout << "product == " << product << '\n';

    std::mt19937 gen(0);
    std::vector<unsigned> large_data;
    std::generate_n(std::back_inserter(large_data), 1'000'000,
                    std::ref(gen));

    // Automatically parallelized sum
    int64_t big_sum = std::reduce(std::execution::par,
                                large_data.cbegin(), large_data.cend(), 0z);
    // stdlibc++: big_sum == 2147988759967286

    std::cout << "big_sum == " << big_sum << '\n';
}
```
std::error_code is a strong type for holding an error code.

Error codes are constructed from an enumeration accompanied by an explanation type derived from std::error_category.

The standard provides std::errc for system errors, std::io_errc for std::iostream and std::future_errc for std::future.
https://compiler-explorer.com/z/aM1oqc5hr

```c++

int main() {
    // Helper for creating error codes that automatically asociates
    // the code with the corresponding category.
    auto err = std::make_error_code(std::errc::not_enough_memory);
    // err.category().name() == "generic"
    // err.message() == "Cannot allocate memory"
    // err.value() == ENOMEM

    // Same as:
    auto err_e = std::error_code(std::to_underlying(std::errc::not_enough_memory), std::generic_category());
    // err == err_e

    assert(err == err_e);

    // Note that the text is implementation specific, 
    // however, specifically for std::errc the values map to errno.

    std::cout << err.category().name() << " error (" << err.value() << ") " << std::quoted(err.message()) << "\n";

    auto future = std::make_error_code(std::future_errc::promise_already_satisfied);
    // future.category().name() == "future"
    // err.message() == "Promise already satisfied"
    
    std::cout << future.category().name() << " error (" << future.value() << ") " << std::quoted(future.message()) << "\n";
}
```
If you need to represent error codes specific to your domain, you can use the std::error_code customization mechanism.

A custom implementation has three parts:

- an enum representing the domain-specific error codes<br />- a corresponding error category that translates the error codes into text descriptions<br />- a mapping from the enum type to the category type
https://compiler-explorer.com/z/aavMrqc94

```c++

// Custom error enum
enum class TransactionError {
    OK = 0,
    TemporaryError = 1,
    PermanentError = 2,
};

// Custom category that provides text description
struct TransactionErrorCategory : std::error_category {
    const char* name() const noexcept override {
        return "transaction";
    }
    std::string message( int condition ) const override {
        using namespace std::string_literals;
        switch(condition) {
            case 0: return "ok"s;
            case 1: return "temporary error, please retry"s;
            case 2: return "permanent error"s;
        }
        std::abort(); // unreacheable
    }
};

// Register the enum as an error code enum
template<> struct std::is_error_code_enum<TransactionError> 
    : public std::true_type{};

// Mapping from error code enum to category
std::error_code make_error_code(TransactionError e) {
    static auto category = TransactionErrorCategory{};
    return std::error_code(std::to_underlying(e), category);
}

// And now we can use it:
std::error_code my_function() noexcept {
    return TransactionError::PermanentError;
}
// And obviously, this also works with std::expected
std::expected<void,std::error_code> my_other_function() noexcept {
    return std::unexpected{TransactionError::TemporaryError};
}

int main() {
    if (auto err = my_function(); err) {
        std::cout << err.category().name() << " : " << err.message() << "\n";
        // prints: "transaction : permanent error"
    }
    if (auto res = my_other_function(); !res) {
        std::cout << res.error().category().name() << " : " << res.error().message() << "\n";
        // prints: "transaction : temporary error, please retry"
    }
}
```
The &quot;Rule of zero&quot; is a class design principle derived from the single responsibility principle.

A class should not define any special member functions unless its sole purpose is managing ownership.
https://compiler-explorer.com/z/qdeTb5en9

```c++

// Piecewise Copy/Move constructors, Copy/Move assignments 
// and destructor will be provided by the compiler.
struct MyClass {
    MyClass(const std::string& label, 
        const std::vector<int>& data) : label_(label), data_(data) {}
private:
    std::string label_;
    std::vector<int> data_;
};

struct MoveOnly {
    MoveOnly(MoveOnly&&) = default;
};

// If one of the members is move-only, the copy constructor
// and copy assignment will not be generated.
struct NoCopyGenerated {
    MoveOnly moveonly_;
};

int main() {
    static_assert(not std::is_copy_constructible_v<NoCopyGenerated>);
}
```
C++20 introduced synchronized streams.

Multiple synchronized streams can be used to write to a single destination stream without introducing data races or interleaving as long as all accesses to that stream are through a synchronized stream.
https://compiler-explorer.com/z/8qMs9M9Yv

```c++

int main() {
    // Safe output to the same stream (here it is std::cout)
    // without introducing data races.
    auto j1 = std::jthread([](){
        std::osyncstream(std::cout) << 
            "We can safely write to the same stream.\n";
    });
    auto j2 = std::jthread([](){
        std::osyncstream(std::cout) << 
            "We can safely write to the same stream.\n";
    });

    // No interleaving, data are sent to std::cout 
    // when osyncstream is destroyed.
    auto j3 = std::jthread([]() {
        {
            std::osyncstream out(std::cout);
            out << "This ";
            out << "will ";
            out << "not ";
            out << "be ";
            out << "interleaved.\n";
        }
        using namespace std::chrono_literals;
        std::this_thread::sleep_for(2000ms);
        {
            std::osyncstream(std::cout) <<
            "Maybe someone said something before me.\n";
        }
    });
    auto j4 = std::jthread([]() {
        std::osyncstream(std::cout) << "Hey!\n";
    });
}
```
std::stack is a simple container adapter providing a stack/LIFO interface.

Besides mitigating the need for recursion, std::stack can be used to implement simple undo functionality.
https://compiler-explorer.com/z/8bxq4aqhq

```c++

// Operations that also return an undo-lambda
auto add(int& state, int operand) {
    state += operand;
    return [operand](int& state) { state -= operand; };
}

auto substract(int& state, int operand) {
    state -= operand;
    return [operand](int& state) { state += operand; };
}

auto multiply(int& state, int operand) {
    state *= operand;
    return [operand](int& state) { state /= operand; };
}

auto divide(int& state, int operand) {
    int orig = state;
    state /= operand;
    return [operand, rem = orig - state * operand](int& state) { 
        state *= operand;
        state += rem;
    };
}

int main() {
    // Apply operations and store the rollback in a std::stack.
    std::stack<std::function<void(int&)>> rollback;

    int state = 0;
    rollback.push(add(state, 10));
    // state == 10

    std::cout << "state == " << state << "\n";

    rollback.push(divide(state, 3));
    // state == 3

    std::cout << "state == " << state << "\n";

    // revert last operation
    rollback.top()(state);
    rollback.pop();
    // state == 10

    std::cout << "state == " << state << "\n";

    rollback.push(multiply(state, 2));
    // state == 20

    std::cout << "state == " << state << "\n";

    while (!rollback.empty()) {
        rollback.top()(state);
        rollback.pop();
    }
    // state == 0

    std::cout << "state == " << state << "\n";
}
```
Unscoped enumerations are simple types that introduce named constants into the scope enclosing the enumeration.

The backing type is automatically selected by the compiler unless explicitly specified.

Note that C++11 Scoped Enumerations are generally preferable (scheduled for tomorrow).
https://compiler-explorer.com/z/a3cP7T5Eo

```c++

// Enumerators are assigned values starting from 0,
// unless overriden, the Enumerators are assgned consecutive values
enum Vegetables { Potato, Tomato, Carrot = 42, Pea, Onion };
// Potato == 0, Tomato == 1, Carrot = 42, Pea == 43, Onion == 44

enum Specific {
    A = 1,
    B = A + 42,
    C = 1, // values may repeat
    D = A + B + C,
    E = std::lcm(13,7) // any constant expression
};
// A == 1, B == 43, C == 1, D == 45, E == 91

// Enumerations can be anonymous and explicitly specify the backing type
enum : unsigned { GlobalConstant = 42 };
// GlobalConstant == 42
// std::underlying_type_t<decltype(GlobalConstant)> == unsigned

// A type that can represent all enumeration values is picked
enum IntLimits {
    Min64Bit = INT64_MIN,
    Max64Bit = INT64_MAX
};
// std::underlying_type_t<IntLimits> == 64bit type

// If no such type exists, the enumeration is ill-formed
enum Impossible {
    X = INT64_MIN,
    Y = UINT64_MAX
};
// Note GCC actually silently picks __int128

int main() {
    std::cout << "Potato == " << Potato << " Tomato == " << Tomato <<
        " Carrot == " << Carrot << " Pea == " << Pea << " Onion == " << Onion << "\n";

    std::cout << "A == " << A << " B == " << B << 
        " C == " << C << " D == " << D << " E == " << E << "\n";

    static_assert(std::is_same_v<std::underlying_type_t<decltype(GlobalConstant)>,unsigned>);

    int v = A; // Enumerators are implicitly convertible 
            // to the backing type
    Specific w = static_cast<Specific>(1);
    // To convert from the underlying type we need a static cast
    // Note, multiple enumerators with the same value do not matter
    // because they are not distinguishable (i.e. A == C)
    static_assert(A == C);

#if (defined(__GNUC__) && !defined(__clang__))
    static_assert(std::is_same_v<std::underlying_type_t<Impossible>, __int128>);
#else
    static_assert(std::is_same_v<std::underlying_type_t<Impossible>, long long>);
#endif
}
```
C++11 introduced scope enumerations.

Enumerators of scoped enumerations are named constants contained within the scope of the enumeration, preventing namespace pollution.

Additionally, scoped enumerations are not implicitly convertible to the underlying type.
https://compiler-explorer.com/z/GboTq66oq

```c++

// The base type (unless specified) is int
enum class Fruit {
    apple, banana, lemon, orange, watermelon
};

enum class Citrus {
    lemon, lime, grapefruit, orange
};

void print(auto);

int main() {
    auto i = Fruit::banana; // decltype(i) == enum Fruit
    std::cout << "i == "; print(i); std::cout << "\n";

    i = Fruit::orange; // OK
    std::cout << "i == "; print(i); std::cout << "\n";

    // i = Citrus::orange; // Wouldn't compile, cannot convert Citrus to Fruit

    auto j = Citrus::grapefruit;
    std::cout << "j == "; print(j); std::cout << "\n";

    using enum Citrus;
    j = orange; // OK, lemon, lime, grapefruit and orange now awailable in local scope
    std::cout << "j == "; print(j); std::cout << "\n";

    // using enum Fruit; // Wouldn't compile, lemon & orange would conflict

    // j = 42; // Wouldn't compile, no implicit conversions
    j = static_cast<Citrus>(2); // Citrus::grapefruit
    std::cout << "j == "; print(j); std::cout << "\n";

    // C++17 direct initialization from integral value
    Citrus k{1}; // OK, k == Citrus::lime
    std::cout << "k == "; print(k); std::cout << "\n";

    int x = static_cast<int>(j); // OK, but we are forcing int
    auto y = static_cast<std::underlying_type_t<decltype(j)>>(j); // Always correct
    // decltype(y) == int
    auto z = std::to_underlying(j); // C++23, we don't have to spell the type
    // decltype(w) == int
    // x == y == z == 2
    std::cout << "x == " << x << ", y == " << y << ", z == " << z << "\n";
}

void print(auto v) {
    if constexpr(std::is_same_v<decltype(v), Fruit>) {
        switch (v) {
            case Fruit::apple: std::cout << "Fruit::apple"; return;
            case Fruit::banana: std::cout << "Fruit::banana"; return;
            case Fruit::lemon: std::cout << "Fruit::lemon"; return;
            case Fruit::orange: std::cout << "Fruit::orange"; return;
            case Fruit::watermelon: std::cout << "Fruit::watermelon"; return;
        }
    } else if constexpr(std::is_same_v<decltype(v), Citrus>) {
        switch (v) {
            case Citrus::lemon: std::cout << "Citrus::lemon"; return;
            case Citrus::lime: std::cout << "Citrus::lime"; return;
            case Citrus::grapefruit: std::cout << "Citrus::grapefruit"; return;
            case Citrus::orange: std::cout << "Citrus::orange"; return;
        }
    }
}
```
std::bind_front is a simpler std::bind alternative, introduced in C++20.

Unlike std::bind, it doesn’t allow for arbitrary reordering of arguments and only allows binding of the leading arguments.

As a consequence, std::bind_front doesn’t suffer from some limitations of std::bind.
https://compiler-explorer.com/z/rrzcbPh1f

```c++

struct Callable {
    void operator()(auto&&...) && {}
};

int main() {
    auto plus = [](int left, int right) { return left + right; };
    auto add10 = std::bind_front(plus, 10);

    auto r = add10(4);
    // r == 14

    std::cout << "r == " << r << "\n";

    // std::bind_front(f, bound_args...)(call_args...) is always equivalent to
    // std::invoke(f, bound_args..., call_args...)
    std::bind_front(Callable{}, 10, 20)();

    // std::bind(Callable{}, 10, 20)(); // Wouldn't compile

    auto var = [](auto&&...) {};
    // std::bind_front doesn't fix number of arguments
    auto bound = std::bind_front(var, 10, 20);
    bound(10); // OK
    bound(10, 20, 30); // OK

    using namespace std::placeholders;
    auto old_bound = std::bind(var, 10, 20, _1, _2, _3);
    old_bound(10, 20, 30); // OK
    // old_bound(10); // Wouldn't compile
}
```
The C++23 std::out_ptr and std::inout_ptr allow interoperability between C++ smart pointers and C-style APIs.

A typical pattern in C APIs is that the (re-)allocating function accepts the handle to be allocated as either T** or void**.

Note that the result of calling std::out_ptr and std::inout_ptr is meant to be a temporary that should not outlive the current expression.
https://compiler-explorer.com/z/64dP3YP6x

```c++

// C API
struct Handle {};
int create_handle(Handle** handle);
int recreate_handle(Handle** handle);
void free_handle(Handle* handle);
int create_handle_ex(int option_a, int option_b, void** handle);

int main() {
    {
    std::unique_ptr<Handle, 
        decltype([](Handle* h) { free_handle(h); })> handle;

    // std::out_ptr for functions that create a handle
    if (int err = create_handle(std::out_ptr(handle)); err != 0)
        throw std::runtime_error("couldn't create handle");

    // std::inout_ptr for functions that first destroy and then create a handle
    if (int err = recreate_handle(std::inout_ptr(handle)); err != 0)
        throw std::runtime_error("couldn't re-create handle");
    
    // void** arguments are also supported
    if (int err = create_handle_ex(1, 2, std::out_ptr(handle)); err != 0)
        throw std::runtime_error("couldn't create handle");
    } // handle freed

    // Also supports std::shared_ptr
    {
    std::shared_ptr<Handle> handle;
    
    if (int err = create_handle(std::out_ptr(handle, free_handle)); err != 0)
        throw std::runtime_error("couldn't create handle");
    } // handle freed
}


int create_handle(Handle** handle) {
    *handle = static_cast<Handle*>(malloc(sizeof(Handle)));
    if (*handle == nullptr) return errno;
    return 0;
}
int recreate_handle(Handle** handle) {
    free(*handle);
    *handle = static_cast<Handle*>(malloc(sizeof(Handle)));
    if (*handle == nullptr) return errno;
    return 0;
}
void free_handle(Handle* handle) {
    free(handle);
}
int create_handle_ex(int, int, void** handle) {
    *handle = malloc(sizeof(Handle));
    if (*handle == nullptr) return errno;
    return 0;
}
```
The std::find_first_of algorithm returns the left-most element from the first range, that matches any of the elements in a second range.

Since neither range is ordered, the complexity is quadratic.
https://compiler-explorer.com/z/r4WhWovK1

```c++

int main() {
    std::vector<int> haystack{1, 2, 3, 4, 5, 6};
    std::vector<int> needle{6, 4, 2};

    // find the first element in haystack matching either 2, 4, or 6
    auto it = std::find_first_of(haystack.begin(), haystack.end(), 
        needle.begin(), needle.end());
    // *it == 2

    std::cout << "*it == " << *it << "\n";

    std::string text = "The quick brown fox jumps over the lazy dog";
    std::string vowels = "aeiou";
    auto first = std::find_first_of(text.begin(), text.end(),
        vowels.begin(), vowels.end(),
            [](char l, char r) { // custom comparator
            return std::tolower(l) == std::tolower(r);
        });
    // *first == 'e'

    std::cout << "*first == " << *first << "\n";
}
```
Standard layout class (struct, union) types offer three important benefits:

- offsetof only works for standard layout types<br />- pointer to a standard layout type and its first member are pointer-interconvertible<br />- if a standard layout union contains standard layout structs, the common initial sequence can be accessed even through non-active members
https://compiler-explorer.com/z/bo48G9r3G

```c++

/* Standard layout type requirements:
- no virtual methods
- no non-static non-standard layout members or base classes
- same access control for all non-static members
- only one class in the hierarchy contains non-static members
*/

struct A { int x; }; // Standard layout
static_assert(std::is_standard_layout_v<A>);

struct B : A {}; // OK, only inheriting standard layout types
static_assert(std::is_standard_layout_v<B>);

struct C { virtual ~C() {} }; // Not SL, virtual method
static_assert(not std::is_standard_layout_v<C>);

struct D : A { int y; }; // Not SL, both D and A contain non-static members
static_assert(not std::is_standard_layout_v<D>);

struct E {
    int x;
    static C c;
}; // Standard layout, C is not SL, but is a static member
static_assert(std::is_standard_layout_v<E>);

struct F {
    int x;
private:
    int y;
}; // Not SL, mixed access
static_assert(not std::is_standard_layout_v<F>);

int main() {
    A a;
    A* ptr = &a;
    int* x = reinterpret_cast<int*>(ptr); // Well defined
    *x = 42;
    assert(*x == ptr->x);

    struct V1 { int version = 1; int data = 42; };
    struct V2 { int version = 2; float data = 4.2; };
    union Versioned {
        V1 v1;
        V2 v2;
    } v{.v2 = V2{}};
    static_assert(std::is_standard_layout_v<V1>);
    static_assert(std::is_standard_layout_v<V2>);
    static_assert(std::is_standard_layout_v<Versioned>);

    switch (v.v1.version) { // Well defined, despite v1 not being active
        case 1: std::cout << "Version 1\n"; break;
        case 2: std::cout << "Version 2\n"; break;
    }
}
```
std::fill and std::generate are simple range-fill algorithms.

The std::fill algorithm fills the supplied range with copies of the provided value.<br />The std::generate algorithm fills the range with the results of successively invoking the provided callable.
https://compiler-explorer.com/z/7dK1h9PPs

```c++

int main() {
    std::vector<int> data(10);

    // Order of invocation is guaranteed
    std::generate(data.begin(), data.end(),
        [iota = 1] mutable {
            return iota++;
        });
    // data == {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}
    std::println("data == {}", data);

    std::fill(data.begin(), data.end(), 42);
    // data == {42, 42, 42, 42, 42, 42, 42, 42, 42, 42}
    std::println("data == {}", data);

    std::vector<int> empty;

    // Both algorithms have a counted variant that takes 
    // an iterator and the number of elements.
    std::fill_n(std::back_inserter(empty), 5, 7);
    // empty == {7, 7, 7, 7, 7}
    std::println("empty == {}", empty);
}
```
std::numeric_limits is a base template whose specializations (for integral and floating point types) expose the various properties of those numerical types (limits and behaviour).

The template can be specialized for user types.
https://compiler-explorer.com/z/rP7nze7P9

```c++

int main() {
    static_assert(std::numeric_limits<int>::is_exact == true);
    static_assert(std::numeric_limits<double>::is_exact == false);

    int v = std::numeric_limits<int>::max();
    // v == 2147483647 (for 32bit int)
    std::println("v == {}", v);

    // Query whether a type has a numeric_limits specialization:
    static_assert(
        std::numeric_limits<std::string>::is_specialized == false);

    int digits = std::numeric_limits<int64_t>::digits10;
    // digits == 18
    // int64_t can represent all 18 digit decimal numbers
    std::println("digits == {}", digits);
}
```
The decltype specifier can be used to obtain the declared type of an entity or the type of an expression.

To treat an entity (technically id-expression or class member access expression) as an expression, it can be surrounded by parenthesis.
https://compiler-explorer.com/z/jnxTs5Wx1

```c++

int main() {
    std::vector<int> data{1,2,3,4,5};

    // lvalue expression -> T&
    static_assert(
        std::is_same_v<decltype(data[2]), int&>);
    static_assert(
        std::is_same_v<decltype(std::as_const(data)[2]), const int&>);
    
    // prvalue expression -> T
    static_assert(
        std::is_same_v<decltype(1+2), int>);

    // xvalue expression -> T&&
    static_assert(
        std::is_same_v<decltype(std::move(data)), std::vector<int>&&>);

    const struct X { int x = 42; } x;

    // id or member -> the declared type
    static_assert(
        std::is_same_v<decltype(x), const X>);
    static_assert(
        std::is_same_v<decltype(x.x), int>);

    // we can treat ids/members as expressions
    static_assert(
        std::is_same_v<decltype((x)), const X&>);
    static_assert(
        std::is_same_v<decltype((x.x)), const int&>);
}
```
When implementing generic code that relies on decltype, you can run into a problem.

If a type cannot be default constructed, we won&#39;t be able to spell an instantiation of that type within an expression.

Using std::declval, we can obtain an lvalue reference without creating a value.

Note that std::declval can only be used in unevaluated contexts (e.g. decltype).
https://compiler-explorer.com/z/KK8frnf51

```c++

template <typename T>
auto fun(T t) { return t+1.0; }

struct MyInt {
    MyInt() = delete; // Not default constructible
    friend int operator+(const MyInt&, double) { return {}; }
    int method() { return {}; }
};

int main() {
    // Get the result type of fun for double and int, all OK
    static_assert(
        std::is_same_v<decltype(fun(double{})), double>);
    static_assert(
        std::is_same_v<decltype(fun(int{})), double>);
    // But we cannot spell the same for MyInt
    // static_assert(
    //    std::is_same_v<decltype(fun(MyInt{})), int>); // Wouldn't compile

    // std::declval allows us to declare a value without creating it
    static_assert(
        std::is_same_v<decltype(fun(std::declval<MyInt>())), int>);

    // Same for methods
    // static_assert(
    //    std::is_same_v<decltype(MyInt{}.method()), int>); // Wouldn't compile
    static_assert(
        std::is_same_v<decltype(std::declval<MyInt>().method()), int>); // OK
}
```
The std::merge algorithm merges two sorted ranges, outputting into a third range.

The algorithm is stable. Equal elements from the first range precede elements from the second range, and the order of equal elements is otherwise unchanged.
https://compiler-explorer.com/z/Y7bxT5csb

```c++

int main() {
    std::vector<int> first{1, 2, 5};
    std::vector<int> second{2, 3, 4};
    std::vector<int> out;

    std::merge(
        begin(first), end(first),   // first input range
        begin(second), end(second), // second input range
        std::back_inserter(out));   // output range
    // out == {1, 2, 2, 3, 4, 5}

    std::println("out == {}", out);

    first = {5, 3, 1};
    second = {4, 2};
    out = {};

    std::ranges::merge(first, second, 
        std::back_inserter(out),
        std::greater<>{}); // Custom comparator
    // out = {5, 4, 3, 2, 1}

    std::println("out == {}", out);

    struct Value {
        int x;
        std::string label;
        bool operator<(const Value& v) const {
            return x < v.x;
        }
    };

    std::vector<Value> a{{0, "a1"}, {0, "a2"}, {1, "a3"}};
    std::vector<Value> b{{0, "b1"}, {1, "b2"}, {1, "b3"}};
    std::vector<Value> c;

    std::ranges::merge(a, b, std::back_inserter(c), std::less<>{});
    // c == {"a1", "a2", "b1", "a3", "b2", "b3"}
    
    std::println("c == {}", c | std::views::transform(&Value::label));
}
```
One thing to keep in mind when implementing templated classes is the handling of dependent names.

Name lookup for non-dependent names is done before the template is instantiated, which can sometimes lead to surprising behaviour.
https://compiler-explorer.com/z/rz44PnPrz

```c++
constexpr auto printer = []{ std::cout << "The global one.\n"; };

struct Printer {
    void printer() { std::cout << "The internal one.\n"; }
};

struct X : Printer {
  	// Printer::printer found by non-ADL lookup
    void fun() { printer(); }
};

template <typename T> struct Y : T {
  	// Non-dependent name, bound to ::printer
    void fun() { printer(); }
};

template <typename T> struct Z : T {
  	// Pulling Printer::printer into current scope
    using T::printer;
    // Early binding will find Z::printer, which a dependent name
    // Non-ADL lookup will then also find Z::printer,
    // which is resolved into Printer::printer
    void fun() { printer(); }
};

template <typename T> struct W : T {
    // Alternative approach, dependent name because W derives from T.
    void fun() { this->printer(); }
};

int main() {
    X{}.fun();
    Y<Printer>{}.fun();
    Z<Printer>{}.fun();
    W<Printer>{}.fun();
}
```
std::byteswap is a simple C++23 utility from the &lt;bit&gt; header that swaps the order of bytes in an integral variable.

In combination with std::endian (C++20), std::byteswap offers a portable solution when serialized data is stored in a different byte order than the native endianness.
https://compiler-explorer.com/z/T9Ejxbfna

```c++
 
int main() {
    uint32_t native_endian = 0x12345678;
    std::println("0x{:x}", native_endian);

    uint32_t flipped_endian = std::byteswap(native_endian);
    // flipped_endian == 0x78563412
    std::println("0x{:x}", flipped_endian);

    // Combining with std::endian
    uint32_t value = 0x0DF0ADBA;
    if constexpr (std::endian::native != std::endian::big)
        value = std::byteswap(value);
    // value == "0xBAADF00D" (on little-endian machines)
    std::println("0x{:X}", value);
}
```
std::latch is one of the simple synchronization primitives introduced with C++20.

Latches are initialized to a specific count, can be atomically decreased and used to block until the count reaches zero.
https://compiler-explorer.com/z/j3qvW488b

```c++

int main() {
    // Simple and inefficient parallel sum
    constexpr uint64_t chunks = 4;
    constexpr uint64_t total = 1024*1024;
    constexpr uint64_t chunk_size = total/chunks;

    // Random, but consistent input
    std::vector<uint32_t> data;
    std::mt19937 rng(1);
    std::generate_n(std::back_inserter(data), total, std::ref(rng));

    std::latch parallel_done(chunks);
    std::array<uint64_t, chunks> sums;
    std::array<std::jthread, chunks> executors;

    for (auto [chunk, result, exec] : 
        std::views::zip(data | std::views::chunk(chunk_size), sums, executors)) {
        // Start a thread for each chunk, storing the result in the corresponding slot
        exec = std::jthread([&, chunk] {
            result = std::ranges::fold_left(chunk, 0uz, std::plus<uint64_t>{});
            // Atomically decrement the latch
            parallel_done.count_down();
        });
    }
    // Block until all threads have produced a sum
    parallel_done.wait();

    // Final reduction, sum up the partial sums
    uint64_t sum = std::ranges::fold_left(sums, 0uz, std::plus<>{});

    std::cout << "sum == " << sum << "\n";
}
```
All templates can be fully specialized (unlike partial specialization, which only works for struct/class and variables).

This mechanism can be used to provide more optimized code for concrete types or as a customization point for libraries.
https://compiler-explorer.com/z/d8PWaxnW4

```c++

// Function templates example
template <typename X>
void fun(const X&) { std::println("base template: const X&"); }

// Full specialization must always fully match the base template
template <> void fun<int>(const int&) { std::println("specialization: int"); }
template <> void fun<bool>(const bool&) { std::println("specialization: bool"); }

// Wouldn't compile, argument mismatch
// template<> void fun<double>(double) {}
// Wouldn't compile, return type mismatch
// template<> bool fun<double>(const double&) {}

// Functions can coexist with matching specializations without conflict
// Resolution will prefer functions over templates
// (but prioritize number of implicit coversions over that)
void fun(const int&) { std::println("function: const int&"); }
void fun(const double&) { std::println("function: const double&"); }

// Used as a customization point

// Base template without implementation
template <typename T> struct DoSomething;
// Full specialization for concrete type
template<> struct DoSomething<int> {
    void do_stuff() { std::println("DoSomething<int>"); }
};

// Customization point with a default

// Base template provides the default value
template <typename T>
constexpr std::string_view label = "unknown type";

// Full specializations for concrete types
template <>
constexpr std::string_view label<unsigned> = "unsigned integer";
template <>
constexpr std::string_view label<std::string_view> = "string view";

int main() {
    fun(4.2); // fun(const double&)
    fun(42); // fun(const int&)
    fun(true); // fun<bool>(const bool&) (specialization)
    fun(long{42}); // fun<long>(const long&) (base template)
    std::println("--");

    DoSomething<int>{}.do_stuff(); // OK
    // DoSomething<double>{}.do_stuff(); // Wouldn't compile
    std::println("--");
    
    std::println(label<unsigned>); // "unsigned integer"
    std::println(label<void>); // "unknown type"
}
```
The std::search_n is a simple algorithm that returns the first instance of n consecutive elements that match the provided value.

The range version conveniently returns the range representing the n consecutive elements, and both versions support a custom comparator.
https://compiler-explorer.com/z/K5Pxvnrvx

```c++

int main() {
    std::vector<int> data{1,7,3,3,9,5,5,5,6,2};

    // First instance of three consecutive '5'
    auto it = std::search_n(data.begin(), data.end(), 3, 5);
    // std::views::counted(it, 3) == {5, 5, 5}

    for (int v : std::views::counted(it, 3))
        std::cout << v << " ";
    std::cout << "\n";

    // Range version returns a range
    auto rng = std::ranges::search_n(data, 2, 3);
    // rng == {3, 3}
    
    for (int v : rng)
        std::cout << v << " ";
    std::cout << "\n";

    // Comparator can be customized
    std::vector<double> approximate{2.4, 2.9, 3.0, 3.8, 3.1};
    auto floor = std::ranges::search_n(approximate, 3, 3.0,
        [](double a, double b) {
            return std::floor(a) == std::floor(b);
        });
    // odd == {3.0, 3.8, 3.1}

    for (double v : floor)
        std::cout << v << " ";
    std::cout << "\n";
}
```
When using auto, the type deduction follows the rules for template type deduction.

One practical consequence of these rules is that auto will never deduce a reference.

Otherwise, the two corner cases to remember are:<br />- auto&amp;&amp; is always deduced and, therefore, a universal/forwarding reference (unlike T&amp;&amp; inside a template)<br />- the behaviour when using list initialization
https://compiler-explorer.com/z/sPnv5G3r4

```c++

int main() {
    int x{};
    const int y{};

    // auto will not deduce a reference
    auto v1 = x;
    static_assert(std::is_same_v<decltype(v1), int>);

    // unless the left side type is a reference
    // the top-level cv-qualifers are discarded
    auto v2 = y;
    static_assert(std::is_same_v<decltype(v2), int>);

    auto &v3 = x; // OK
    static_assert(std::is_same_v<decltype(v3), int&>);

    // if the left side type is a reference, the cv-qualifiers are kept
    auto &v4 = y;
    static_assert(std::is_same_v<decltype(v4), const int&>);

    // auto &v5 = int{}; // Wouldn't compile
    // Cannot bind int&& to auto&

    const auto &v6 = int{}; // OK
    static_assert(std::is_same_v<decltype(v6), const int&>);


    // Special case: universal/forwarding reference
    // Universal references maintain value category
    auto&& u1 = x;
    static_assert(std::is_same_v<decltype(u1), int&>);

    auto&& u2 = std::move(x); // or auto&& u2 = int{};
    static_assert(std::is_same_v<decltype(u2), int&&>);

    auto&& u3 = y;
    static_assert(std::is_same_v<decltype(u3), const int&>);

    auto&& u4 = std::move(y); // OK, but not a useful category
    static_assert(std::is_same_v<decltype(u4), const int&&>);


    // Special case: list initialization
    auto l1 = {1}; // Copy-list initialization -> initializer list
    static_assert(std::is_same_v<decltype(l1), std::initializer_list<int>>);
    
    auto l2 = {1,2,3}; // OK
    static_assert(std::is_same_v<decltype(l2), std::initializer_list<int>>);
    
    auto l3{1}; // Direct list initialization -> the element type
    static_assert(std::is_same_v<decltype(l3), int>);

    // auto l4{1,2,3}; // Wouldn't compile
    // only one element allowed for direct list initialization
}
```
The std::move and std::move_backward algorithms are complementary algorithms to std::copy and std::copy_backward that move each element from the source range to the destination.

All variants will fall back to a copy when operating with immovable types.
https://compiler-explorer.com/z/jjfhez8G9

```c++

struct Movable {
    int v = 42;
    Movable() = default;
    Movable(Movable&& other) : v(std::exchange(other.v, -1)) {}
    Movable& operator=(Movable&& other) {
        v = std::exchange(other.v, -1);
        return *this;
    }
};

void format_range(std::string_view label, auto& rng, auto projection);

int main() {
    std::vector<Movable> data;
    data.emplace_back(); data.emplace_back(); data.emplace_back();

    std::vector<Movable> out(3);
    // std::ranges::copy(data, out.begin()); // Wouldn't compile
    std::ranges::move(data, out.begin()); // OK
    // out == {{42}, {42}, {42}}, data == {{-1}, {-1}, {-1}}

    format_range("out", out, &Movable::v);
    format_range("data", data, &Movable::v);

    // Overlapping ranges
    std::vector<int> rng{1,2,3,4,5,6,7,8,9};
    // The begining of output range cannot overlap with input range
    //     | input range |
    // | output range | 
    std::move(
        rng.begin()+4, rng.end(), // input
        rng.begin()); // output
    // rng == {5, 6, 7, 8, 9, _, _, _, _}

    format_range("rng", rng, std::identity{});

    rng = {1,2,3,4,5,6,7,8,9};
    // The begining of input range cannot overlap with output range
    // | input range | 
    //    | output range |
    std::move_backward(
        rng.begin(), rng.end()-4, // input
        rng.end()); // output
    // rng == {_, _, _, _, 1, 2, 3, 4, 5}

    format_range("rng", rng, std::identity{});

    // Copy-only types:
    struct CopyOnly {};
    const std::vector<CopyOnly> src(5); // immutable source
    std::vector<CopyOnly> dst(5);

    // We cannot move from an immutable container, defaults to copy
    std::move(src.begin(), src.end(), dst.begin());
    std::ranges::move(src, dst.begin());
}


void format_range(std::string_view label, auto& rng, auto projection) {
    std::string delim = "";
    std::cout << label << " == {";
    for (auto &v : rng)
        std::cout << std::exchange(delim, ", ") << std::invoke(projection, v);
    std::cout << "}\n";
}
```
Before C++14, looking up elements in an ordered container was only possible using the type matching the key.

Because of this, lookups using a non-matching type required conversion and construction of a temporary key.

Notably, for std::string, this typically also means an allocation.
https://compiler-explorer.com/z/vooYhvGcc

```c++

struct Tracer {
    static bool trace;
    Tracer() { trace = true; }
    ~Tracer() { trace = false; }
};

void* operator new(std::size_t sz);

int main() {
{
std::println("Without heterogenous lookup:");
std::map<std::string, std::string> lookup;
lookup.insert({"a", "label_a"});
lookup.insert({"b", "label_b"});
lookup.insert({"c", "label_c"});

Tracer on;
// const char* needs to be converted to std::string,
// for a long key this requires heap allocation
auto it = lookup.find("long_key_that_requires_allocation");
}


{
std::println("With heterogenous lookup:");
// std::less<void> will deduce arguments independently
std::map<std::string, std::string, std::less<>> lookup;
lookup.insert({"a", "label_a"});
lookup.insert({"b", "label_b"});
lookup.insert({"c", "label_c"});

Tracer on;
// no conversion needed, calls templated find() added in C++14
// std::less<> can compare std::string and const char*
auto it = lookup.find(+"long_key_that_requires_allocation");

// Tip: the unary plus changes the deduced type from char[34]
// to const char*, or preferably use
// "long_key_that_requires_allocation"sv for a std::string_view literal
}
}

bool Tracer::trace = false;

void* operator new(std::size_t sz) {
    if (Tracer::trace)
        std::printf("allocation size = %zu\n", sz);
    return std::malloc(sz);
}
```
The std::reverse_copy is a simple algorithm that copies the source bidirectional range in reverse order into the output range.

Typically, std::reverse_copy offers a more natural solution than using std::copy with reverse iterators.
https://compiler-explorer.com/z/fqbq7a534

```c++

int main() {
    std::vector<int> data{1,2,3,4,5,6,7,8,9};
    std::vector<int> out;
    
    std::reverse_copy(data.begin(), data.end(),
        std::back_inserter(out));
    // out == {9, 8, 7, 6, 5, 4, 3, 2, 1}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";

    out.clear();
    // same as:
    std::copy(data.rbegin(), data.rend(), 
        std::back_inserter(out));
    // out == {9, 8, 7, 6, 5, 4, 3, 2, 1}
    
    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";
}
```
One problem you can run into with auto type-deduction is when using auto to deduce the result type of a function.

The solution can be decltype(auto), which, unsurprisingly, follows the decltype rules for determining the type.
https://compiler-explorer.com/z/sz5zhY8sv

```c++

// Same as auto v = 1;, i.e. int
auto f1() { return 1; }
// Same as int x; auto v = x;, i.e. int
auto f2() { static int x = 1; return x; }
// Same as auto v = f2();, i.e. int
auto f3() { return f2(); }
// All three functions deduce int
// f2 (and transitively f3) results in a copy

int get_temp() { return 1; }
int& get_var() { static int x = 1; return x; }

// Same as decltype(get_temp()), i.e. int
decltype(auto) f4() { return get_temp(); }
// Same as decltype(get_var()), i.e. int&
decltype(auto) f5() { return get_var(); }

// Notably this is importantly when working with generic callables
decltype(auto) wrap(auto&& arg, auto projection) {
    return projection(std::forward<decltype(arg)>(arg));
}

int main() {
    static_assert(std::is_same_v<decltype(f1()), int>);
    static_assert(std::is_same_v<decltype(f2()), int>);
    static_assert(std::is_same_v<decltype(f3()), int>);


    static_assert(std::is_same_v<decltype(get_temp()), int>);
    static_assert(std::is_same_v<decltype(f4()), int>);
    static_assert(std::is_same_v<decltype(get_var()), int&>);
    static_assert(std::is_same_v<decltype(f5()), int&>);

    // Using std::identity: wrap(int{}, identity) 
    // int&& argument, forwarded and passed through as int&&
    static_assert(std::is_same_v<decltype(wrap(int{}, std::identity{})), int&&>);

    int x = 42;
    // Using std::identity{}: wrap(x, identity)
    // int& argument, forwarded and passed through as int&
    static_assert(std::is_same_v<decltype(wrap(x, std::identity{})), int&>);

    auto custom = [](int arg) { return arg; };
    // Using a custom projection that returns int: wrap(42, custom)
    // int&& argument, bound into int, returned as int
    static_assert(std::is_same_v<decltype(wrap(42, custom)), int>);
}
```
The std::find_end algorithm operates similarly to std::search; however, instead of returning the first instance of a subrange in a range, it returns the last.
https://compiler-explorer.com/z/Tqj1557dn

```c++

int main() {
    std::vector<int> haystack{1,2,3,4,5,1,2,3,4,5};
    std::vector<int> needle{3,4};

    auto it = std::find_end(
        haystack.begin(), haystack.end(),
        needle.begin(), needle.end());
    // (it - haystack.begin()) == 7
    std::println("(it - haystack.begin()) == {}", (it - haystack.begin()));   

    // Ranges version returns the found instance as a subrange
    auto rng = std::ranges::find_end(haystack, needle);
    // rng == {3,4}
    std::println("rng == {}", rng);

    // Both versions support custom comparator
    std::string sentence = "Word word WORD wORD";
    std::string word = "word";

    auto case_sen = std::ranges::find_end(sentence, word);
    // case_sen == "word";
    auto case_ins = std::ranges::find_end(sentence, word, [](char l, char r){
        return std::tolower(l) == std::tolower(r);
    });
    // case_ins == "wORD"

    std::println("case_sen == {}", case_sen);
    std::println("case_ins == {}", case_ins);
}
```
The C++23 std::bind_back is a complementary utility to std::bind_front, which binds the last n arguments of a callable.

A call to std::bind_back(Callable, bound args...)(call args...) is equivalent to std::invoke(Callable, call args..., bound args...).
https://compiler-explorer.com/z/decj9KhTh

```c++

// Alternative to complex default arguments/wrapping lambdas
struct MainArg{};
void do_stuff(MainArg, int option1, int option2, int option3) {}

constexpr auto do_stuff_one_way = std::bind_back(do_stuff, 2, 1, -3);
// do_stuff_one_way(MainArg{});
constexpr auto do_stuff_another_way = std::bind_back(do_stuff, -7, 6, 2);
// do_stuff_another_way(MainArg{});

int main() {
    auto f = [](int a, int b, int c, int d, int e, int f) {
        std::cout << a << " " << b << " " << c << " " << 
            d << " " << e << " " << f << "\n";
    };
    auto bound = std::bind_back(f, 1, 2, 3);
    bound(4,5,6);
    // prints: 4 5 6 1 2 3

    do_stuff_one_way(MainArg{});
    do_stuff_another_way(MainArg{});
}
```
While using std::sort to sort a range is reasonably fast, it is still wasteful if you only require the top few elements.

The std::partial_sort will only sort the top k elements with O(n*logk) time complexity and will even outperform std::nth_element if k is small compared to n.
https://compiler-explorer.com/z/Kjvj6bE9a

```c++

int main() {
    std::vector<int> data{3,9,8,5,1,2,7,4,6};
    
    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // partially sort, so that the first three elements are sorted
    std::partial_sort(data.begin(), data.begin()+3, data.end());
    // data == {1, 2, 3, ...}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    // range version with a custom comparator
    std::ranges::partial_sort(data, data.begin()+3, std::greater<>{});
    // data == {9, 8, 7, ...}

    for (auto v : data)
        std::cout << v << " ";
    std::cout << "\n";

    struct V { int v; };
    std::vector<V> nested{{3},{9},{8},{5},{1},{2},{7},{4},{6}};

    // projecting each element to the v member
    std::ranges::partial_sort(nested, nested.begin()+3, std::less<>{}, &V::v);

    for (auto v : nested)
        std::cout << v.v << " ";
    std::cout << "\n";
}
```
While heterogeneous lookup in ordered containers has been available since C++14, we only got heterogeneous lookup for unordered containers in C++20.

To enable heterogeneous lookup, the container must be instantiated with a hasher and an equality comparator that support all types we want to use for lookup.
https://compiler-explorer.com/z/f7a9cosYn

```c++

struct string_hash {
    // required to denote a transparent hash
    using is_transparent = void;
    // Hash operations required to be consistent: 
    // a == b => hash(a) == hash(b)
    size_t operator()(const char *txt) const {
        return std::hash<std::string_view>{}(txt);
    }
    size_t operator()(std::string_view txt) const {
        return std::hash<std::string_view>{}(txt);
    }
    size_t operator()(const std::string &txt) const {
        return std::hash<std::string>{}(txt);
    }
};

// Custom key type:
struct Wrapped {
    int64_t value;
    static auto make(int64_t v) { return Wrapped{v}; }
private:
    // Private constructor to demonstrate that this isn't a conversion
    Wrapped(int64_t v) : value(v) {};
};

// Custom transparent hash:
struct wrapped_hash {
    using is_transparent = void;
    size_t operator()(int64_t value) const {
        return std::hash<int64_t>{}(value);
    }
    size_t operator()(const Wrapped& wrapped) const {
        return std::hash<int64_t>{}(wrapped.value);
    }
};

// Custom comparator:
struct wrapped_cmp {
    using is_transparent = void;
    bool operator()(int64_t left, const Wrapped& right) const {
        return left == right.value; 
    }
    bool operator()(const Wrapped& left, const Wrapped& right) const {
        return left.value == right.value; 
    }
};

int main() {
    // Unordered map with a transparent hasher and std::equal_to
    // (since std::string already provides operator==)
    std::unordered_map<std::string, std::string, 
                    string_hash, std::equal_to<>> string_map;
    string_map.insert_or_assign("a", "label_a");
    string_map.insert_or_assign("b", "label_b");

    // No conversion and therefore no allocation here:
    auto i = string_map.find("long_key_that_requires_allocation");
    // i == string_map.end()

    std::cout << std::boolalpha << "(i == string_map.end()) == " << (i == string_map.end()) << "\n";

    // Unordered map with both the hasher and comparator customized
    std::unordered_map<Wrapped, std::string,
                    wrapped_hash, wrapped_cmp> data;
    data.insert_or_assign(Wrapped::make(10), "Hello World!");
    data.insert_or_assign(Wrapped::make(5), "Goodbye!");

    // No conversion, lookup directly using int64_t
    auto j = data.find(5z);
    // j->first == Wrapped{5}, j->second == "Goodbye!"

    std::cout << "j->first.value == " << j->first.value << ", j->second == " << j->second << "\n";
}
```
The C++17 std::from_chars is a low-level integer and floating-point parsing function.

Importantly, std::from_chars doesn&#39;t require or assume null termination and operates on a range of characters delimited using two pointers.

This behaviour is useful when parsing from data streams, memory or when working with std::string_view.
https://compiler-explorer.com/z/58z33dG96

```c++

int main() {
    std::string_view i1 = "1234";
    int o1{};
    std::from_chars(i1.data(), i1.data()+i1.size(), o1);
    // o1 == 1234

    std::println("o1 == {}\n", o1);

    // The function returns information about potential errors
    // and the end of parsed section
    std::string_view i2 = "123Hello";
    int o2{};
    {
    auto [ptr, err] = std::from_chars(i2.data(), i2.data()+i2.size(), o2);
    // *ptr == 'H', err == Success
    std::println("*ptr == {}, err == {}", *ptr, std::make_error_condition(err).message());
    std::println("o2 == {}", o2);
    }

    std::string_view i3 = "-4";
    unsigned o3{};
    {
    auto [ptr, err] = std::from_chars(i3.data(), i3.data()+i3.size(), o3);
    // *ptr == '-', err == Invalid argument
    std::println("*ptr == {}, err == {}", *ptr, std::make_error_condition(err).message());
    }

    int o4{};
    {
    auto [ptr, err] = std::from_chars(i3.data(), i3.data()+i3.size(), o4);
    // ptr == i3.end(), err == Success
    std::println("(ptr == i3.end) == {}, err == {}", (ptr == i3.end()), std::make_error_condition(err).message());
    std::println("o4 == {}\n", o4);
    }

    // Base can be specified (but the function doesn't parse the prefix)
    std::string_view i4 = "0xFE"; // hexadecimal
    std::string_view i5 = "077";  // octal
    int o5{};
    {
    i4 = i4.substr(2); // Skip the hexadecimal prefix
    std::from_chars(i4.data(), i4.data()+i4.size(), o5, 16);
    // o5 == 0xfe
    std::println("o5 == {} / 0x{:x}", o5, o5);

    i5 = i5.substr(1); // Skip the octal prefix
    std::from_chars(i5.data(), i5.data()+i5.size(), o5, 8);
    // o5 == 077
    std::println("o5 == {} / 0{:o}\n", o5, o5);
    }

    // Floating point support
    std::string_view i6 = "3.14";
    double o6{};
    std::from_chars(i6.data(), i6.data()+i6.size(), o6);
    std::println("o6 == {}", o6);

    std::string_view i7 = "0.1e-15";
    double o7{};
    std::from_chars(i7.data(), i7.data()+i7.size(), o7);
    std::println("o7 == {}", o7);
}
```
The std::swap_ranges algorithm does a piecewise swap of two ranges of elements.

Consequently, std::swap_ranges has linear complexity but allows for swapping heterogeneous ranges.

Additionally, when working with containers that dynamically allocate memory, a swap might not be a suitable choice, especially with custom allocators or memory resources.
https://compiler-explorer.com/z/849v6sqq5

```c++

int main() {
    std::vector<int> first{1,2,3,4};
    std::vector<int> second{9,8,7,6};
    second.reserve(128);

    // first == {1,2,3,4}, second == {9,8,7,6}
    // first.capacity() == 4, second.capacity() == 128
    std::println("first == {}, second == {}", first, second);
    std::println("first.capacity() == {}, second.capacity() == {}\n",
        first.capacity(), second.capacity());

    std::ranges::swap(first, second);
    // first == {9,8,7,6}, second == {1,2,3,4}
    // first.capacity() == 128, second.capacity() == 4
    std::println("first == {}, second == {}", first, second);
    std::println("first.capacity() == {}, second.capacity() == {}\n",
        first.capacity(), second.capacity());

    std::ranges::swap_ranges(first, second);
    // first == {1,2,3,4}, second == {9,8,7,6}
    // first.capacity() == 128, second.capacity() == 4
    std::println("first == {}, second == {}", first, second);
    std::println("first.capacity() == {}, second.capacity() == {}\n",
        first.capacity(), second.capacity());

    // Unlike swap, swap_ranges can swap between ranges of different types
    std::array<int, 4> arr{0,0,0,0};

    std::ranges::swap_ranges(first, arr);
    // first == {0,0,0,0}, arr == {1,2,3,4}
    std::println("first == {}, arr == {}", first, arr);
}
```
C++26 introduced a set of saturating arithmetic operations: addition, subtraction, multiplication, division and cast.

If the specified integral type cannot represent the result of the operation, the result is instead std::numeric_limits::min() or std::numeric_limits::max() (whichever is closer).
https://compiler-explorer.com/z/z4bjj81vP

```c++

int main() {
    int x = std::numeric_limits<int>::max();
    // x + 1 is UB
    int y = std::add_sat(x,1); // OK
    // y == x

    std::println("x == {}, x + 1 == {}", x, y);

    int a = std::numeric_limits<int>::min();
    // a / -1 is UB
    int b = std::div_sat(a,-1); // OK
    // b == std::numeric_limits<int>::max()

    std::println("a == {}, a / -1 == {}", a, b);

    uint16_t c = std::saturate_cast<uint16_t>(-1);
    // c == 0

    std::println("c == {}", c);
}
```
During normal program termination, any registered callbacks using std::atexit are executed as part of the cleanup. The callbacks follow the initialization and destruction rules from C++11.

For example, if a construction of a static object was ordered before a callback is registered, the destruction of that object will be sequenced after the callback is invoked.
https://compiler-explorer.com/z/o5zqq79sf

```c++

void callback_a() {
    std::println(stderr, "callback A");
}

void callback_b() {
    std::println(stderr, "callback B");
}

struct Global {
    ~Global() { std::println(stderr, "~Global()"); }
};

Global global;

int main() {
    std::atexit(callback_a);
    std::atexit(callback_b);
    // Implicit call to std::exit(0);
    
    // Calling std::Exit(int); or std::quick_exit(int); 
    // will not invoke the callbacks
}
```
The C++20 &lt;format&gt; introduced a modern approach to text formatting.

When formatting to a buffer or a stream, the std::string returning std::format may not be suitable.

The library offers two overloads that instead output the formatted text through an output iterator: std::format_to and std::format_to_n, which also limits the number of characters written.
https://compiler-explorer.com/z/nPej3Gvsb

```c++

int main() {
    std::vector<char> buffer;
    int x = 42;
    // formatting into a dynamic buffer
    std::format_to(std::back_inserter(buffer), "x == {}", x);
    
    std::string_view str1(buffer.begin(), buffer.end());
    // str1 == "x == 42"
    std::cout << str1 << "\n";

    // Also works for output to streams
    std::format_to(std::ostream_iterator<char>(std::cout), "x == {}\n", x);
    // prints: x == 42

    std::array<char, 32> static_buffer;
    // formatting into a static buffer
    std::format_to_n(static_buffer.begin(), 32,
        "Today is {}, Expected temperature is {} Celsius",
        "Tuesday", 24);
    
    std::string_view str2(static_buffer.begin(), static_buffer.end());
    // str2 == "Today is Tuesday, Expected tempe"
    std::cout << str2 << "\n";
}
```
On POSIX systems, the environment variables can be accessed through the global variable char** environ. This variable is also typically passed as the third argument to main.

For a portable solution, the standard offers the std::getenv function. Note that while the function returns char*, modifying the returned string is UB.
https://compiler-explorer.com/z/dnd58jhEo

```c++

int main() {
    std::println("LD_LIBRARY_PATH:");

    // Get the LD_LIBRARY_PATH variable
    if (const char* record = std::getenv("LD_LIBRARY_PATH");
        record != nullptr) { // nullptr if the variable is not set

        std::string_view str = record;
        // Iterate over the chunks, delimited by ':'
        for (auto chunk : str | std::views::split(':')) {
            std::println("\t{}", std::string_view(chunk.begin(), chunk.end()));
        }
    }
}
```
A current limitation of concepts is that we cannot pass concepts as arguments to templates. At least not directly.

The following trick by Filip Sajdak allows us to wrap concepts in lambdas and then pass those lambdas as non-type template arguments. With the help of an indirection concept, we can then check types against this wrapped concept.
https://compiler-explorer.com/z/bqW9jc57z

```c++

// Indirect concept check through a lambda
template <auto wrapper, typename T>
concept satisfies_wrapped_concept = requires {
    { wrapper.template operator()<T>() };
};

// Template parametrized using a wrapped concept and a type
template <auto WrappedConcept, typename T>
// Limit T to types that satisfy the passed in concept
requires satisfies_wrapped_concept<WrappedConcept, T>
struct Box {};

// Demonstration of use:
template <typename T>
// Box parametrized for integral types
// A templated lambda, with the template argument constrained with a concept
using BoxForIntegrals = Box<[]<std::integral>{},T>;
template <typename T>
// Box parametrized for floating point types
using BoxForFloats = Box<[]<std::floating_point>{},T>;

int main() {
    BoxForIntegrals<int> a; // OK
    // BoxForIntegrals<float> b; // Wouldn't compile
    BoxForFloats<float> c; // OK
    // BoxForFloats<int> d; // Wouldn't compile
}
```
The std::forward is a conditional r-value (specifically x-value) cast.

std::forward is primarily designed to work in conjunction with universal references and will only cast to an r-value if the template argument isn&#39;t an l-value reference type.
https://compiler-explorer.com/z/E5dcPh6T3

```c++

struct X {};

void some_func(const X&) { std::println("const X&"); }
void some_func(X&&) { std::println("X&&"); }

// Typical use case
void forwarding_function(auto&& arg) {
    // perfect forwarding, as if we called some_func directly
    some_func(std::forward<decltype(arg)>(arg));
}

int main() {
    X x;
    // r-value cast, calls some_func(X&&)
    some_func(std::forward<decltype(x)>(x));

    X&& y = X{};
    // r-value cast, calls some_func(X&&)
    some_func(std::forward<decltype(y)>(y));

    // note that names of variables are always l-values
    some_func(y); // calls some_func(const X&)

    X& z = x;
    // no cast, calls some_func(const X&)
    some_func(std::forward<decltype(z)>(z));

    const X& w = x;
    // no cast, calls some_func(const X&)
    some_func(std::forward<decltype(w)>(w));
}
```
When we need to copy the content of a range, replacing some of the elements as we copy, we can use the std::replace_copy and std::replace_copy_if algorithms.

Both algorithms support projections in their range versions.
https://compiler-explorer.com/z/xoaeerY9M

```c++

int main() {
    std::vector<int> in{1,2,3,4,5};
    std::vector<int> out(5);

    std::replace_copy(in.begin(), in.end(), // copy from in
        out.begin(), // to out
        2, -1); // replacing elements of value 2 with -1
    // out == {1, -1, 3, 4, 5}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";

    std::replace_copy_if(in.begin(), in.end(), // copy from in
        out.begin(), // to out
        [](int v) { return v % 2 != 0; }, // replacing odd elements
        0); // with zero
    // out == {0, 2, 0, 4, 0}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << "\n";

    std::vector<std::string> labels{"a", "b", "hello", "bye", "e"};
    std::vector<std::string> out2(5);

    // Example with a projection
    std::ranges::replace_copy(labels, out2.begin(),
        1, "---", // Replace strings of length 1 with "---"
        [](const std::string& s) { return s.length(); });
    // out2 == {"---", "---", "hello", "bye", "---"}
    
    for (auto v : out2)
        std::cout << std::quoted(v) << " ";
    std::cout << "\n";
}
```
The std::shared_ptr is a powerful yet heavy, ref-counted smart pointer.

Different instances of std::shared_ptr that point to the same block can be safely handled without thread synchronization.

std::shared_ptr is useful for data handoff in multi-threaded code. A producer can relinquish ownership while ensuring that consumers do not lose access to the data.
https://compiler-explorer.com/z/MPnf5nsen

```c++

int main() {
    std::vector<std::jthread> runners(4);

    {
    // some simple data generation (not that relevant)
    std::vector<uint32_t> data_in;
    std::mt19937 rng;
    std::generate_n(std::back_inserter(data_in), 
                    1024*1024, std::ref(rng));

    // Create a shared pointer from the vector.
    // Note the const, making the data immutable.
    auto shared =
        std::make_shared<const std::vector<uint32_t>>(std::move(data_in));

    // Start the threads, giving each thread a copy of the shared_ptr
    for (auto &thread : runners) {
        thread = std::jthread([shared]() {
            // accessing const-methods of STL containers is thread-safe
            for (auto v : *shared) { 
                // process data...
            }
        });
    }
    }

    // 4 runner threads are all running, but the local reference
    // to the data is no longer held.

    // The data will be released once all runners finish, thus releasing
    // their references to the data, i.e. the ref-count reaches zero.

    // std::jthread auto-joins on destruction, or we can do it explicitly:
    for (auto &thread : runners) {
        thread.join();
    }
}
```
The C++20 std::views::elements takes a range of tuple-like objects and produces a view over the n-th element from each tuple.

The concept of tuple-like was formalized in C++23 and includes std::array, std::complex (C++26), std::pair, std::tuple and std::ranges::subrange.
https://compiler-explorer.com/z/nnnTYzK8E

```c++

int main() {
    std::vector<std::pair<int,double>> data{{1,2.7}, {3, 4.2}, {-1, 3.3}};

    for (auto v : data | std::views::elements<0>) {
        std::print("{} ", v);
    }
    std::println("");

    for (auto v : data | std::views::elements<1>) {
        std::print("{} ", v);
    }
    std::println("");

    std::array<std::array<int,3>,3> grid{1,2,3,4,5,6,7,8,9};
    for (auto v : grid | std::views::elements<2>) {
        std::print("{} ", v);
    }
    std::println("");
}
```
When using std::invoke in generic code, you can run into situations when the result type of the invocation is inconvenient.

The C++23 std::invoke_r allows the return type to be explicitly specified, avoiding cumbersome wrapping code.
https://compiler-explorer.com/z/81rvT13Mz

```c++

struct Box {
    int value;
};

struct Base {} base;
struct Derived : Base {} derived;
 
Base& fun(int) { return base; }
Derived& fun(double) { return derived; }

int main() {
    Box x{42};
    auto&& a = std::invoke(&Box::value, x); // OK
    auto&& b = std::invoke(&Box::value, Box{42}); // Bad, danling reference

    // std::invoke_r allows the return type to be specified
    auto&& c = std::invoke_r<int>(&Box::value, Box{42}); // OK

    // void is also valid, discarding any potential result
    std::invoke_r<void>(&Box::value, Box{42});

    // Collapsing covariant return types

    // Wrapper for the overload set
    auto wrapped = [](auto arg) -> decltype(auto) { return fun(arg); };

    auto& i1 = std::invoke(wrapped, 42); // calls fun(int)
    // decltype(i1) == Base&
    static_assert(std::is_same_v<decltype(i1), Base&>);

    auto& i2 = std::invoke(wrapped, 4.2); // calls fun(double)
    // decltype(i2) == Derived&
    static_assert(std::is_same_v<decltype(i2), Derived&>);

    auto& i3 = std::invoke_r<Base&>(wrapped, 42); // calls fun(int)
    // decltype(i3) == Base&
    static_assert(std::is_same_v<decltype(i3), Base&>);

    auto& i4 = std::invoke_r<Base&>(wrapped, 4.2); // calls fun(double)
    // decltype(i4) == Base&
    static_assert(std::is_same_v<decltype(i4), Base&>);
}
```
Since C++26, we can finally avoid the cumbersome need to name objects even when the name doesn&#39;t matter using the placeholder name _.

For variables with dynamic lifetimes, non-static members, lambda captures and structured bindings, the _ is permitted to redefine existing instances.

If the _ has a unique meaning (no redefinition), it can still be referenced.
https://compiler-explorer.com/z/TzfbP1vx8

```c++

int main() {
    std::map<int, int> data{{1,2},{2,1},{3,4},{4,3}}; 

    // Iterate over values, ignoring keys
    for (auto& [_, value] : data) {
        std::print("{} ", value);
    }
    std::println("");

    std::mutex mux;
    {
        // RAII-only objects do not need to be named
        auto _ = std::unique_lock{mux}; 
        /*
        critical section
        */
    } // lock released

    // Multiple _ named variables with dynamic storage duration,
    // non-static member variables, bindings and captures
    // can redeclare previous _ in the same scope
    auto _ = 42;
    auto _ = 7;

    struct S {
        int _;
        int _;
    } s{42, 7};

    // Unique (i.e. not re-declared) _ can still be referenced
    {
        auto _ = [](auto _) { return _; };
        int x = _(42);
        // x == 42;
        std::println("x == {}", x);
    }
}
```
When working with C++17 memory resources, you might want to implement a custom resource.

Fortunately, this is straightforward. The PMR library offers an abstract base class std::pmr::memory_resource.

A custom memory resource has to derive from this class and implement the private methods do_allocate, do_deallocate and do_is_equal.
https://compiler-explorer.com/z/qcc6eeqK7

```c++

// A simple tracing memory resource that uses an upstream resource
// for allocation, but prints information about the allocated
// or deallocated memory.
struct Tracer : std::pmr::memory_resource {
    Tracer(std::string_view label, std::pmr::memory_resource* upstream) : label_(label), up_(upstream) {}
private:
    // allocation implementation
    void* do_allocate(std::size_t bytes, std::size_t alignment) override {
        void* ptr = up_->allocate(bytes, alignment);
        std::println("[{}] allocated {}, {} bytes with {} byte allignment",
            label_, ptr, bytes, alignment);
        return ptr;
    }
    // deallocation implementation
    void do_deallocate(void* p, std::size_t bytes, std::size_t alignment) override {
        std::println("[{}] deallocating {}, {} bytes with {} byte alignment",
            label_, p, bytes, alignment);
        up_->deallocate(p, bytes, alignment);
    }
    // used to determine whether the container using this resource can move
    // or has to fallback to a copy, we cannot move across resources
    bool do_is_equal(const std::pmr::memory_resource& other) const noexcept override {
        // Tracer is transparent, so a Tracer(&res) == res
        if (up_ == &other)
            return true;
        // Two different tracers are equivalent
        // if they point to the same backing resource
        if (auto* ptr = dynamic_cast<const Tracer*>(&other); ptr != nullptr) {
            return up_ == ptr->up_;
        }
        return false;
    }  

    std::string label_;
    std::pmr::memory_resource* up_;
};

int main() {
    Tracer system("system", std::pmr::new_delete_resource());
    std::pmr::unsynchronized_pool_resource pool_res(&system);
    Tracer pool("pool", &pool_res);

    std::pmr::list<std::pmr::string> list(&pool);
    for (auto _ : std::views::iota(0, 128)) {
        list.push_back("This is going to allocate because it doesn't fit into SSO.");
    }
}
```
The C++23 std::unreachable is a tool for injecting undefined behaviour into a program. A typical use case is in an else branch or the default case of a switch.

Compilers can use this undefined behaviour and optimise the corresponding branch away.

While we can achieve the same behaviour with [[assume(false)]], std::unreachable provides a more readable and logical name.
https://compiler-explorer.com/z/xh6h3EMqv

```c++

__attribute__((noinline)) int fun1(int x) {
    switch (x) {
        case 1: return 0;
        case 2: return 1;
        case 4: return 2;
        case 8: return 3;
    }
    return -1;
}

__attribute__((noinline)) int fun2(int x) {
    switch (x) {
        case 1: return 0;
        case 2: return 1;
        case 4: return 2;
        case 8: return 3;
        default: std::unreachable();
    }
    return -1;
}

__attribute__((noinline)) int fun3(int x) {
    return x / 4;
}

__attribute__((noinline)) int fun4(int x) {
    if (x < 0) std::unreachable();
    // same as [[assume(x < 0)]];
    return x / 4;
}


```
std::weak_ptr is a smart pointer type that can be constructed from a std::shared_ptr instance.

A std::weak_ptr instance does not count towards owning references and will therefore not keep the held instance alive; however, it can be safely upgraded to a std::shared_ptr.
https://compiler-explorer.com/z/zPnThWqq3

```c++

struct Item {
    // Holding a weak pointer does not imply ownership.
    std::weak_ptr<Item> next;
  
    void touch_next() {
        // Attempt to upgrade to a std::shared_ptr that will
        // only live for the duration of the if statement.
        if (auto owned = next.lock(); owned != nullptr)
            std::cout << "Next in line is " << owned.get() << "\n";
    }  
};

struct Something{};
struct Observer {
    // Observer can safely keep track of objects
    // without affecting their lifetime.
    std::list<std::weak_ptr<Something>> global_objects;
    
    void register_global(std::weak_ptr<Something> global) {
        global_objects.push_back(std::move(global));
    }
    
    void observe() {
        auto it = global_objects.begin();
        while (it != global_objects.end()) {
            if (auto owned = it->lock(); owned != nullptr) {
                std::cout << "Observing " << owned.get() << "\n";
                it++;
            } else {
                it = global_objects.erase(it); // already gone, drop
            }
        }
    }
};

int main() {
    {
    std::vector<std::shared_ptr<Item>> data;
    // Fill with 16 items that loop around
    std::generate_n(std::back_inserter(data), 16, 
                    [](){ return std::make_shared<Item>(); });
    for (size_t i = 0; i < data.size(); i++) {
        if (i+1 < data.size())
            data[i]->next = data[i+1]; // point to the next
        else
            data[i]->next = data[0]; // cycling back to the first
    }

    data[5]->touch_next();
    // Example output: Next in line is 0x16ca4e0
    // data[6].get() == 0x16ca4e0
    }
    // All items are correctly destructed at this point.

    {
    Observer observer;
    auto global1 = std::make_shared<Something>();
    auto global2 = std::make_shared<Something>();

    observer.register_global(global1);
    observer.register_global(global2);

    observer.observe(); // observes both global objects

    global1.reset();
    observer.observe(); // observes global2
    // observer.global_objects.size() == 1
    }
}
```
A simple option to handle formatted I/O is using istream and ostream iterators.

However, both iterators will interpret special characters.

To address this, the standard library also offers istreambuf and ostreambuf iterators that instead operate on the stream buffer, without additional semantics.
https://compiler-explorer.com/z/r3PaW9G8x

```c++

struct Custom {
    // When using istreambuf/ostreambuf iterators to implement 
    // stream extraction/insertion, make sure to correctly set
    // eof state when end iterator is reached (and fail state 
    // when parsing fails).
    friend std::istream& operator>>(std::istream& s, Custom&) {
        // Do all the implementation specific operations on 
        // the stream so that it's ready for a manual read.
        std::istream::sentry all_good{s, true};
        if (not all_good) return s;

        std::istreambuf_iterator<char> it{s};
        std::istreambuf_iterator<char> end{};

        for (int i = 0; i < 2; ++i, ++it) {
            // If we are at eof and did not manage to read two characters
            if (it == end) {
                // Set the eof flag
                s.setstate(std::ios_base::eofbit);
                return s;
            }

            // Check that the first two characters match the first
            // line: #include <fstream>
            char c = *it;
            if (i == 0 && c != '#') {
                s.setstate(std::ios_base::failbit);
                return s;
            }
            if (i == 1 && c != 'i') {
                s.setstate(std::ios_base::failbit);
                return s;
            }
        }

        return s;
    }
};

int main() {
    // Open a file
    std::ifstream in("./example.cpp", std::ios::binary);

    // Read the content of the file into a vector
    std::vector<char> file{std::istreambuf_iterator<char>(in), 
        std::istreambuf_iterator<char>()};
    // Default initialized iterator represents EOF

    // Write the content of the vector into standard output
    std::ranges::copy(file, 
        std::ostreambuf_iterator<char>(std::cout));

    in.seekg(0);
    Custom custom;
    in >> custom;

    std::cout << std::boolalpha << "\nin.good() == " << in.good() << "\n";
}
```
Correctly forwarding the value category for a member of a compound type before C++23 was cumbersome.

Forunately C++23 introduced the std::forward_like, which makes this operation a lot simpler.

The std::forward_like is also essential when using the &quot;deducing this&quot; feature.
https://compiler-explorer.com/z/a5h6M184s

```c++

struct Wrapper {
    int member;
};

void fun(const int&) {
    std::println("const int&");
}
void fun(int&) {
    std::println("int&");
}
void fun(int&&) {
    std::println("int&&");
}

// Only calls fun(int&) or fun(const int&)
void extract1(auto&& wrapper) {
    fun(wrapper.member);
}

// Correct, but cumbersome
void extract2(auto&& wrapper) {
    if constexpr (std::is_rvalue_reference_v<decltype(wrapper)>) {
        fun(std::move(wrapper.member));
    } else {
        fun(wrapper.member);
    }
}

// Using C++23 forward_like
void extract3(auto&& wrapper) {
    fun(std::forward_like<decltype(wrapper)>(wrapper.member));
}

// Typical use case
struct MyType {
    auto&& get(this auto&& self) {
        // One getter variant covering all value categories
        return std::forward_like<decltype(self)>(self.data);
    }
    int data;
};

int main() {
    Wrapper w;
    extract1(w); // calls fun(int&)
    extract1(std::as_const(w)); // calls fun(const int&)
    extract1(Wrapper{}); // calls fun(int&)
    std::println("--");
    
    extract2(w); // calls fun(int&)
    extract2(std::as_const(w)); // calls fun(const int&)
    extract2(Wrapper{}); // calls fun(int&&)
    std::println("--");

    extract3(w); // calls fun(int&)
    extract3(std::as_const(w)); // calls fun(const int&)
    extract3(Wrapper{}); // calls fun(int&&)
}
```
The std::tie is a C++11 utility from the tuple header that creates a tuple of lvalue references to the arguments.

While structured bindings in C++17 replaced the primary use case, std::tie remains useful when combined with std::ignore or as a tool to simplify the implementation of comparisons.
https://compiler-explorer.com/z/bTz7KqPja

```c++

struct Coord {
    int x;
    int y;

    // We can use std::tie to simplify some operations
    bool operator<(const Coord& other) const {
        return std::tie(x, y) < std::tie(other.x, other.y);
    }
    bool operator==(const Coord& other) const {
        return std::tie(x, y) == std::tie(other.x, other.y);
    }
    friend void swap(Coord& l, Coord& r) {
        std::swap(std::tie(l.x, l.y), std::tie(r.x, r.y));
    }
};

std::pair<double,int> fun() { return {4.2, 42}; }

int main() {
    int x{};
    const double y{};

    // Creates a tuple of lvalue references
    auto t1 = std::tie(x, y);
    // decltype(t1) == std::tuple<int&, const double&>

    static_assert(std::is_same_v<decltype(t1), std::tuple<int&, const double&>>);

    // std::tie can be used as a less elegant version of structured bindings
    double i{}; int j{};
    std::tie(i,j) = fun();
    // i == 4.2, j == 42

    std::println("i == {}, j == {}", i, j);

    // std::ignore can be combined with std::tie to skip over some fields
    std::tie(std::ignore, j) = std::pair{0.0, 7};
    // i == 4.2, j == 7

    std::println("i == {}, j == {}", i, j);

    // We can use std::tie to re-use structured binding identifiers
    auto v = std::pair{4.2, 42};
    auto [m, n] = v; // copy of v
    // m == 4.2, n == 42

    std::println("v == [{}, {}], m == {}, n == {}", v.first, v.second, m, n);

    std::tie(m, n) = std::pair{1.1, 2};
    // m == 1.1, n == 2
    std::println("v == [{}, {}], m == {}, n == {}", v.first, v.second, m, n);

    auto& [p, q] = v; // in this case we have a reference to v
    std::tie(p, q) = std::pair{1.1, 2};
    // v == {1.1, 2}, p == 1.1, q == 2
    std::println("v == [{}, {}], p == {}, q == {}", v.first, v.second, p, q);

    Coord a{1, 1}, b{1, 2};
    std::println("(a < b) == {}", a < b);
    std::println("(a == b) == {}", a == b);
    swap(a, b);

    std::println("a == [{},{}], b == [{},{}]", a.x, a.y, b.x, b.y);
}
```
The std::reference_wrapper is a simple indirection wrapper that can implicitly convert to the wrapped type.

std::reference_wrapper has special interactions with std::make_pair and std::make_tuple (where the type is deduced as T&amp;) and can force reference semantics for APIs where the default behaviour involves a copy.
https://compiler-explorer.com/z/GehcKhd14

```c++

void fun(const std::vector<int>&, int) {}

int main() {
    std::vector<int> data{1,2,3,4,5};

    // The result of bind_front has value semantics, 
    // meaning the results are stored by copy.
    auto fn1 = std::bind_front(fun, data);
    fn1(42);

    // std::ref returns a std::reference_wrapper, avoiding the copy.
    // However, now fn2 cannot outlive data.
    auto fn2 = std::bind_front(fun, std::ref(data));
    fn2(42);

    int x{};
    // std::make_pair and std::make_tuple have special handling
    // for std::reference_wrapper
    auto t = std::make_tuple(x, std::ref(x), std::cref(x));
    // decltype(t) == std::tuple<int, int&, const int&>
    static_assert(std::is_same_v<decltype(t), std::tuple<int,int&,const int&>>);

    std::vector<int> rand;
    std::mt19937 rng;
    // We could wrap rng in a lambda, but we can also use
    // a std::reference_wrapper, which also provides operator()
    std::generate_n(std::back_inserter(rand), 10, std::ref(rng));

    // Wouldn't compile:
    // std::reference_wrapper<int> wrap1(42); // Cannot wrap temporaries
    // std::reference_wrapper<int> wrap2; // Cannot be "null"
}
```
The canonical use case for std::shared_ptr is a cache.

Items need to be removed from the cache to make space for fresh data, but we cannot invalidate memory that is still in use by in-progress operations.
https://compiler-explorer.com/z/W63Pfa1xa

```c++

template <typename Data, uint32_t capacity = 64>
struct LRU {
    using ProviderType = std::function<std::unique_ptr<Data>(const std::string&)>;
    LRU(ProviderType provider) : provider_(std::move(provider)) {}

    // Get the data by key, either from the cache or from the canonical provider.
    std::shared_ptr<const Data> get(const std::string& key) {
        std::scoped_lock lock(mux_); // acquire lock, we will be mutating state
        if (auto it = map_.find(key); it != map_.end()) { // happy path first
            bump_to_most_recently_used(it->second);
            return it->second->data;
        } else {
            // if the key isn't in the cache, use the provider 
            // to generate the data
            return insert_from_provider(key); 
        }
    }

private:
    ProviderType provider_;
    struct Store {
        std::string key;
        std::shared_ptr<const Data> data;
    };
    std::list<Store> stable_;
    std::unordered_map<std::string, typename std::list<Store>::iterator> map_;
    std::mutex mux_;

    void bump_to_most_recently_used(std::list<Store>::iterator it) {
        stable_.splice(stable_.begin(), stable_, it);
    }

    void drop_least_recently_used() {
        auto it = std::prev(stable_.end());
        map_.erase(it->key);
        stable_.erase(it);
    }

    std::shared_ptr<const Data> insert_from_provider(const std::string& key) {
        if (map_.size() == capacity)
            drop_least_recently_used();
        auto it = stable_.insert(stable_.begin(), {
            key,
            provider_(key)
        });
        map_.insert_or_assign(key, it);
        return it->data;
    }
};


struct Blob {
    std::string data;
};

int main() {
    LRU<Blob> cache([](const std::string& key) -> std::unique_ptr<Blob> {
        return std::make_unique<Blob>(std::format("{}->{}", key, key));
    });

    auto v1 = cache.get("cat"); // not in cache
    std::println("v1->data == \"{}\"", v1->data);
    auto v2 = cache.get("cat"); // in cache
    std::println("(v1 == v2) == {}", (v1 == v2));
}
```
The std::views::all may seem a bit pointless, producing a view of all elements in a range.

However, std::views::all will produce different types based on the value category of the argument and whether it is a borrowed range, ensuring that we don&#39;t end up with a dangling view.

The resulting type can also be accessed through a helper std::views::all_t.
https://compiler-explorer.com/z/rx4jWK3fr

```c++

int main() {
    std::vector<int> data{1,2,3,4,5};
    
    // constructed from lvalue, v1 will be a ref_view
    auto v1 = std::views::all(data);
    // decltype(v1) == std::ranges::ref_view<std::vector<int>>
    static_assert(std::is_same_v<decltype(v1), std::ranges::ref_view<std::vector<int>>>);

    // we can also obtain the type using std::views::all_t
    using t1 = std::views::all_t<std::vector<int>&>;
    // t1 == std::ranges::ref_view<std::vector<int>>
    static_assert(std::is_same_v<t1, std::ranges::ref_view<std::vector<int>>>);

    // constructed from rvalue, v2 will be an owning_view
    auto v2 = std::views::all(std::vector<int>{1,2,3,4,5});
    // decltype(v2) == std::ranges::owning_view<std::vector<int>>
    static_assert(std::is_same_v<decltype(v2), std::ranges::owning_view<std::vector<int>>>);

    using t2 = std::views::all_t<std::vector<int>&&>;
    // t2 == std::ranges::owning_view<std::vector<int>>
    static_assert(std::is_same_v<t2, std::ranges::owning_view<std::vector<int>>>);

    // Wouldn't compile, owning_view does not support copy, only move
    // auto v3 = v2;

    // constructed from borrowed range, v4 will be std::span
    auto v4 = std::views::all(std::span(data));
    // decltype(v4) == std::span<int>
    static_assert(std::is_same_v<decltype(v4), std::span<int>>);

    using t4 = std::views::all_t<std::span<int>>;
    // t4 == std::span<int>
    static_assert(std::is_same_v<t4, std::span<int>>);   
}
```
The three function objects std::logical_and, std::logical_or and std::logical_not model the functionality of the corresponding logical operators &amp;&amp; (and), || (or), ! (not).

Note that the arguments of a function call are all evaluated before the function is called in an unspecified order. This contrasts the short-circuit evaluation of &amp;&amp; and || operators.
https://compiler-explorer.com/z/E76cEcTMb

```c++

int main() {
    std::vector<std::optional<int>> data{1,{},2,3,{},4,5,6,{},{}};

    // Partition the vector with empty optionals first
    std::partition(data.begin(), data.end(), std::logical_not<>{});
    // same as:
    std::partition(data.begin(), data.end(),
        [](const auto& o) { return not o.has_value(); });

    for (auto& v : data)
        if (v)
            std::cout << *v << ", ";
        else
            std::cout << "{}, ";
    std::cout << '\n';

    std::vector<bool> in1{false, true, true, false, false, true, false, true};
    std::vector<bool> in2{true, true, false, false, true, false, true, false};
    std::vector<bool> out1(in1.size());
    
    // element-wise reduction using logical AND from in1 and in2 to out1
    std::transform(in1.begin(), in1.end(), in2.begin(), out1.begin(),
        std::logical_and<>{});
    for (auto v : out1)
        std::cout << v;
    std::cout << "\n";

    std::vector<bool> out2(in1.size());

    // element-wise reduction using logical AND from in1 and in2 to out1
    std::transform(in1.begin(), in1.end(), in2.begin(), out2.begin(),
        std::logical_or<>{});

    for (auto v : out2)
        std::cout << v;
    std::cout << "\n";

    std::cout << "\n";

    // Important: because std::logical_and, std::logical_or are 
    // function objects, they do not have short-circuit logic 
    // of && and || operators.
    auto fn1 = []{ std::cout << "fn1()\n"; return true; };
    auto fn2 = []{ std::cout << "fn2()\n"; return true; };
    auto fn3 = []{ std::cout << "fn3()\n"; return false; };
    auto fn4 = []{ std::cout << "fn4()\n"; return false; };
   
    // Only fn1 is called
    bool r1 = fn1() || fn2();
    std::cout << "\n";
    // Both fn1 and fn2 is called in unspecified order
    bool r2 = std::logical_or<>{}(fn1(), fn2());
    std::cout << "\n";
    // Only fn3 is called
    bool r3 = fn3() && fn4();
    std::cout << "\n";
    // Both fn3 and fn4 is called in unspecified order
    bool r4 = std::logical_and<>{}(fn3(), fn4());
}
```
The std::_Exit function can be used to exit a process without invoking destructors and without raising the SIGABRT signal.

The standard also offers std::quit_exit, which additionally calls the functions registered through std::at_quick_exit, after which it calls std::_Exit.
https://compiler-explorer.com/z/MdvE8Msfo

```c++

struct User {
    std::string label;
    ~User() { std::cerr << "Destructing " << label << "\n";  }
};

User global("global object");

int main() {
    // Destructors of static, dynamic and thread
    // local objects are not called.
    User local("local object");

    // Functions registered using at_quick_exit 
    // will be executed in reverse order.
    std::at_quick_exit([]{ std::cerr << "at exit [1]\n"; });
    std::at_quick_exit([]{ std::cerr << "at exit [2]\n"; });

    // Only functions registered using at_quick_exit will be called.
    std::quick_exit(EXIT_SUCCESS);
    // calls std::_Exit(EXIT_SUCCESS); internally
}
```
The two C++23 range algorithms, std::ranges::starts_with and std::ranges::ends_with, implement prefix and suffix checks for ranges.

std::ranges::starts_with can operate on any range, std::ranges::ends_with requires at least a forward range.
https://compiler-explorer.com/z/a5ssW7YEs

```c++

int main() {
    std::vector<int> haystack{1,2,3,4,5,6,7,8,9};
    std::forward_list<int> prefix{1,2,3};
    std::forward_list<int> suffix{7,8,9};

    bool pref = std::ranges::starts_with(haystack, prefix);
    bool suff = std::ranges::ends_with(haystack, suffix);
    // pref == true, suff == true

    std::println("pref == {}, suff == {}", pref, suff);

    bool nomatch = std::ranges::starts_with(prefix, suffix);
    // nomatch == false

    std::println("nomatch == {}", nomatch);

    // With projections and custom comparator
    bool proj = std::ranges::starts_with(haystack, suffix, 
        [](int l, int r) { return std::abs(l-r) <= 2; }, // custom comparator
        [](int l) { return l + 2; }, // project to {3,4,5...}
        [](int r) { return r - 2; }); // project to {5,6,7}
    // proj == true

    std::println("proj == {}", proj);
}
```
The C++23 std::mdspan is a view over a contiguous sequence of elements, providing the interface of a multidimensional array over these elements.

The array dimensions can be specified both statically and dynamically. Additionally, std::mdspan supports data layouts, including the option to define custom layouts.
https://compiler-explorer.com/z/o4a9x1oh4

```c++

int main() {
    std::vector<int> arr(24);
    std::iota(arr.begin(), arr.end(), 0);

    // 3d array with runtime specified dimensions
    auto v1 = std::mdspan(arr.data(), 3, 4, 2);

    // Size of each dimension can be accessed using the extent method
    for (auto i : std::views::iota(0uz, v1.extent(0))) {
        for (auto j : std::views::iota(0uz, v1.extent(1))) {
            for (auto k : std::views::iota(0uz, v1.extent(2))) {
                // Elements can be accessed using the C++23 
                // multi-dimensional subscript operator
                // v1[i,j,k] == arr[i*(4*2)+j*2+k]
                std::print("{} ", v1[i,j,k]);
            }
            std::println("");
        }
        std::println("");
    }

    // We can define a custom type of index and custom dimensions
    using custom_extents = std::extents<uint8_t, std::dynamic_extent, 12>;
    // std::mdspan with uint8_t as the index type, runtime sized first dimension
    // and static second dimension
    auto v2 = std::mdspan<int, custom_extents>(arr.data(), arr.size()/12);

    // Because the second dimension is statically sized,
    // v2.extent(1) is a constant expression
    static_assert(v2.extent(1) == 12);
    // decltype(v2.extent(1)) == uint8_t
    static_assert(std::is_same_v<decltype(v2.extent(1)),uint8_t>);

    // Statically sized dimensions will generally lead to better codegen
    for (uint8_t i = 0; i != v2.extent(0); ++i)
        // The length of this loop is known at compile time
        for (uint8_t j = 0; j != v2.extent(1); ++j)
            std::print("{} ", v2[i,j]);
}
```
The format library (C++20) offers text formatting with an interface that is more in line with the C printf-style functions (and their formatting options).

However, unlike the C formatting functions, the format is parsed and checked at compile time, leading to better safety and performance.
https://compiler-explorer.com/z/f6crdPo33

```c++

int main() {
    // Basic format returns a std::string
    std::string simple = std::format("The {} horsemen of {}", 
                                    4, "apocalypse");
    // simple == "The 4 horsemen of apocalypse"

    std::cout << "simple == " << std::quoted(simple) << "\n";

    auto is_even = [](int v) { return v % 2 == 0; };
    for (auto v : std::views::iota(1, 7)) {
        // Output using an output iterator
        std::format_to(std::ostreambuf_iterator(std::cout),
            "is_even({}) == {}\n",
            v, is_even(v)); // bool is by default formatted as true/false
    }
    /* prints:
    is_even(1) == false
    is_even(2) == true
    is_even(3) == false
    is_even(4) == true
    is_even(5) == false
    is_even(6) == true
    */

    // Typical formatting options are present (e.g. precision)
    auto pi5 = std::format("{:.5}", std::numbers::pi);
    // pi5 == "3.1416"

    std::cout << "pi5 == " << std::quoted(pi5) << "\n";

    int width = 10;
    int precision = 3;
    // Formatting arguments can be provided as part of the argument list
    auto pivar = std::format("{: ^{}.{}}", 
                            std::numbers::pi, 
                            width, precision);
    // print with width specified by the 3rd argument, precision specified
    // by 4th argument, centre-align with ' ' as filler
    // pivar == "  3.14  "

    std::cout << "pivar == " << std::quoted(pivar) << "\n";
}
```
Compile-time constant expressions are not permitted to invoke undefined behaviour. This includes constexpr functions that are evaluated at compile-time.

This property can be used to statically test code, ensuring that the code doesn&#39;t invoke undefined behaviour.
https://compiler-explorer.com/z/3x78r4nMr

```c++

constexpr int midpoint(int a, int b) {
    return (a + b)/2; // can overflow, int overlow is UB
}

constexpr int generate() {
    std::vector<int> data = {1};
    auto it = data.begin();
    for (int i = 0; i < 10; i++)
        data.push_back(i); // invalidates it
    return *it; // accessing invalid iterator
}

constexpr int process() {
    int* buffer = new int[10];
    for (int i = 0; i < 10; i++)
        buffer[i] = i;
    int sum = 0;
    for (int i = 0; i < 10; i++)
        sum += i;
    return sum; // we memory leak buffer
}

constexpr int cnt_space(const char* str, size_t sz) {
    int cnt = 0;
    for (size_t i = 0; i < sz ; ++i) {
        if (str[i] == ' ') ++cnt; // out-of-bounds
    }
    return cnt;
}

int main() {
    constexpr int a = std::numeric_limits<int>::max();
    constexpr int b = a - 2;
    constexpr int c = a - 1;

    // Wouldn't compile: "overflow in constant expression"
    static_assert(midpoint(a, b) == c);
    // Wouldn't compile "use of storage after deallocation"
    static_assert(generate() == 1);
    // Wouldn't compile "storage has not been deallocated"
    static_assert(process() == 45);
    // Wouldn't compile "array subscript value '8' is outside 
    //                   the bounds of array type 'const char [8]'"
    static_assert(cnt_space("a b c d", 9) == 3);
}
```
Implementing generic C++ code can be tricky, as any operation can potentially throw.

Notably, when a strong exception guarantee is required, this can significantly complicate code and lead to runtime overhead (or even a change in big-O complexity).

Fortunately, C++20 concepts can be used to enforce `noexcept` guarantees at compile-time.
https://compiler-explorer.com/z/dTxT3bshM

```c++

struct UnsafeType {
    UnsafeType() = default;
    UnsafeType(UnsafeType&&) {}
    UnsafeType& operator=(UnsafeType&&) { return *this; }
};

template <typename T>
void unsafe_swap(T& left, T& right) {
    auto tmp = std::move(left);
    left = std::move(right); // What happens if this move throws?
    // left was moved from, and moving it back might throw again
    right = std::move(tmp);
}

struct SafeType {
    SafeType() = default;
    SafeType(SafeType&&) noexcept {}
    SafeType& operator=(SafeType&&) noexcept { return *this; }
};

template <typename T>
requires requires (T& a, T& b) {
 	// move assignment is valid and doesn't throw
	{ a = std::move(b) } noexcept;
}
void safe_swap(T& left, T& right) {
    auto tmp = std::move(left);
    left = std::move(right);
    right = std::move(tmp);
}

int main() {
    SafeType a, b;
    safe_swap(a, b); // OK

    UnsafeType x, y;
    // Wouldn't compile:
    // safe_swap(x, y); // UnsafeType doesn't satisfy the noexcept requirement
}
```
The C++20 standard introduced a new set of smart pointer construction functions: make_unique_for_overwrite, make_shared_for_overwrite and allocate_shared_for_overwrite.

These variants default-initialize the allocated memory, unlike the value-initialization of previous variants.

This avoids duplicate initialization of POD types when the memory is immediately overwritten.
https://compiler-explorer.com/z/dMYeGdb8G

```c++

int main() {
    auto p1 = std::make_unique_for_overwrite<int>();
    // decltype(p1) == std::unique_ptr<int>, *p1 == indeterminate value
    static_assert(std::is_same_v<decltype(p1), std::unique_ptr<int>>);

    auto p2 = std::make_shared_for_overwrite<int>();
    // decltype(p2) == std::shared_ptr<int>, *p2 == indeterminate value
    static_assert(std::is_same_v<decltype(p2), std::shared_ptr<int>>);

    std::pmr::monotonic_buffer_resource mr;
    std::pmr::polymorphic_allocator<int> alloc{&mr};
    auto p3 = std::allocate_shared_for_overwrite<int>(alloc);
    // decltype(p3) == std::shared_ptr<int>, *p3 == indeterminate value
    static_assert(std::is_same_v<decltype(p3), std::shared_ptr<int>>);

    // Overloads also support default-initialized arrays
    auto p4 = std::make_unique_for_overwrite<int[]>(7); // array with 7 elements
    // decltype(p4) == std::unique_ptr<int[]>, p4[0] == indeterminate value
    static_assert(std::is_same_v<decltype(p4), std::unique_ptr<int[]>>);

    auto p5 = std::make_shared_for_overwrite<int[]>(7);
    // decltype(p5) == std::shared_ptr<int[]>, p5[0] == indeterminate value
    static_assert(std::is_same_v<decltype(p5), std::shared_ptr<int[]>>);

    auto p6 = std::allocate_shared_for_overwrite<int[]>(alloc, 7);
    // decltype(p6) == std::shared_ptr<int[]>, p6[0] == indeterminate value
    static_assert(std::is_same_v<decltype(p6), std::shared_ptr<int[]>>);
}
```
The C++20 std::counting_semaphore is a synchronization primitive that can limit the number of concurrent threads accessing a shared resource.

The typical use cases overlap with std::condition_variable; however, std::counting_semaphore offers a more straightforward interface and potentially better performance.
https://compiler-explorer.com/z/9daM1xaT3

```c++

struct Data {};

// Simple unbounded thread-safe many<->many producer/consumer queue
struct WorkQueue {
    WorkQueue() : queue_{}, mux_{}, sem_{0} {}
    void push(std::convertible_to<Data> auto&& data) {
        // Push a new element into the queue
        {
            auto _ = std::lock_guard(mux_);
            queue_.push_back(std::forward<decltype(data)>(data));
        }
        // Atomically increase the counter in the semaphore.
        // If any threads are blocked on acquire, they will be notified.
        sem_.release();
    }
    Data pop() {
        // Try to atomically decrease the counter in the semaphore.
        // If the counter is already 0, blocks.
        sem_.acquire();

        // At this point we are guaranteed available data,
        // still need to synchronize against other consumers.
        auto _ = std::lock_guard(mux_);
        Data result = std::move(queue_.front());
        queue_.pop_front();
        return result;
    }
private:
    std::deque<Data> queue_;
    std::mutex mux_;
    std::counting_semaphore<> sem_;
};

int main() {
    using namespace std::chrono;
    WorkQueue q;

    auto producer = std::jthread{[&q]{
        std::this_thread::sleep_for(200ms);
        std::println("Producer: publishing data");
        q.push(Data{});
        std::this_thread::sleep_for(200ms);
        std::println("Producer: publishing data");
        q.push(Data{});
        std::println("Producer: publishing data");
        q.push(Data{});
        std::println("Producer: publishing data");
        q.push(Data{});
    }};
    auto consumer1 = std::jthread{[&q]{
        std::println("Consumer1: attempting to read data");
        auto _ = q.pop();
        std::println("Consumer1: succeeded in reading data");
        std::println("Consumer1: attempting to read data");
        auto _ = q.pop();
        std::println("Consumer1: succeeded in reading data");
    }};
    auto consumer2 = std::jthread{[&q]{
        std::println("Consumer2: attempting to read data");
        auto _ = q.pop();
        std::println("Consumer2: succeeded in reading data");
        std::println("Consumer2: attempting to read data");
        auto _ = q.pop();
        std::println("Consumer2: succeeded in reading data");
    }};
}
```
The std::pair is a simple heterogeneous container for storing two elements, conceptually similar to std::tuple.

std::pair supports the std::tuple interface: std::get, std::tuple_size, std::tuple_element.

If you care about performance and work with trivially copyable types, consider an aggregate instead because std::pair isn&#39;t trivially copyable.
https://compiler-explorer.com/z/a963s6sGb

```c++

int main() {
    std::pair<int,double> v{7, 4.2};
    auto [a, b] = v; // deconstruct using structured binding
    // a == 7, b == 4.2

    // Pair interface:
    v.first = 0;
    v.second = 3.14;
    // v == {0, 3.14}

    // Tuple interface:
    std::get<0>(v) = 7;
    std::get<1>(v) = 4.2;
    // v == {7, 4.2}
    // std::tuple_size_v<decltype(v)> == 2
    // std::tuple_element_t<0, decltype(v)> == int
    // std::tuple_element_t<1, decltype(v)> == double

    static_assert(std::tuple_size_v<decltype(v)> == 2);
    static_assert(std::is_same_v<
        std::tuple_element_t<0, decltype(v)>, int>);
    static_assert(std::is_same_v<
        std::tuple_element_t<1, decltype(v)>, double>);

    // Not trivially copyable
    static_assert(not std::is_trivially_copyable_v<decltype(v)>);

    // Potential alternative
    struct simple_pair {
        int first;
        double second;
    } w{7, 42};
    static_assert(std::is_trivially_copyable_v<simple_pair>);

    // std::pair can be constructed using std::make_pair
    // the type is deduced using std::decay, 
    // except for std::reference_wrapper
    int arr[3] = {0,1,2};
    std::pair p1 = std::make_pair(arr, arr);
    // decltype(p1) == std::pair<int*,int*>

    static_assert(std::is_same_v<
        decltype(p1), std::pair<int*,int*>>);

    int x{7};
    std::pair p2 = std::make_pair(x, std::reference_wrapper(x));
    // decltype(p2) == std::pair<int, int&>

    static_assert(std::is_same_v<
        decltype(p2), std::pair<int,int&>>);

    // Note that CTAD guides do not have special handling 
    // for std::reference_wrapper
    std::pair p3{x, std::reference_wrapper(x)};
    // decltype(p3) == std::pair<int, std::reference_wrapper<int>>

    static_assert(std::is_same_v<
        decltype(p3), std::pair<int,std::reference_wrapper<int>>>);
}
```
ZOMBIES is a testing mnemonic introduced by James Grenning.

ZOMBIES represents a two-dimensional checklist to ensure you do not forget any important angles that should be covered with unit tests.

x-axis: Z - zero, O - one, M - many<br />y-axis: B - boundary, I - interface, E - exceptions<br />across: S - simple scenarios/solutions
https://compiler-explorer.com/z/399YrY758

```c++

// ZOMBIES
// First axis:  Z - zero, O - one, M - many
// Second axis: B - boundary, I - interface, E - exceptions
// Cross axis:  S - simple scenarios/solutions

int main() {
// Testing std::sort as a demonstration:
{   // Zero, Boundary
    std::vector<int> data;
    std::sort(data.begin(), data.end()); // Implicit test, it doesn't crash/throw
}
{
    // One, Boundary
    std::vector<int> data{1};
    std::sort(data.begin(), data.end());
    assert(data[0] == 1);
}
{
    // Many
    std::vector<int> data{5, 2, 4, 1, 3};
    std::sort(data.begin(), data.end());
    auto cmp = {1, 2, 3, 4, 5};
    assert(std::equal(data.begin(), data.end(), cmp.begin()));
}
{
    // Interface
    std::vector<int> data{5, 2, 4, 1, 3};
    std::sort(data.begin(), data.end(), [](int l, int r) {
        if (l%2 != r%2) return l%2 < r%2; // even before odd
        return l < r;
    });
    auto cmp = {2, 4, 1, 3, 5};
    assert(std::equal(data.begin(), data.end(), cmp.begin()));
}
}
```
Exceptions are one of the error-handling mechanisms available in C++. They are the only viable mechanism for reporting errors in constructors or operators.

Exceptions do not introduce an overhead for the happy path; however, exceptions do increase the binary&#39;s size, and because handling a thrown exception is relatively slow, they are only suitable for rare occurrences.
https://compiler-explorer.com/z/qMWTWhcWh

```c++

// Custom exception type derived from std::exception
struct MyException : std::exception {
    MyException(int payload) 
        : payload_{std::format("MyException{{{}}}", payload)} {}
    ~MyException() override = default;
    const char* what() const noexcept override {
        return payload_.c_str();
    }
private:
    std::string payload_;
};

int main() {
    auto fun = [](int v){
        try { 
            // Exceptions thrown in this scope can 
            // be caught in the catch block.
            if (v == 0)
                throw std::runtime_error("Exceptions can have descriptions");
            if (v == 1)
                throw MyException{42};
            if (v == 2)
                throw std::invalid_argument("Another error");
            if (v == 3)
                // We can throw anything, although if you don't want to
                // use std::exception, you should still throw a base
                // class specific to your library.
                throw int{42};
        } catch (const std::runtime_error& e) { // always catch by const-ref
            // The thrown exception will be matched against the catch
            // clauses in the order listed.
            std::cerr << "Runtime Error: " << e.what() << "\n";
        } catch (const std::exception& e) {
            // If for whatever reason you don't want to use separate
            // catch clauses, we can still dynamic cast manually
            if (const MyException* err = dynamic_cast<const MyException*>(&e);
                err != nullptr) {
                std::cerr << err->what() << "\n";
            } else {
                std::cerr << "Error: " << e.what() << "\n";
            }
        } catch (...) { // the catch-all clause
            std::cerr << "Unknown error" << "\n";
            // Exceptions can be thrown or re-thrown from 
            // catch blocks.
            throw std::logic_error("Unhandled error");
        }
        // If an exception does not match any of the catch clauses
        // it will be propagated to the parent frame.
    };

    fun(0);
    fun(1);
    fun(2);
    fun(3);
}
```
The std::async and its accompanying launch policies std::launch::deferred and std::launch::async is a tool for running a callable either lazily or asynchronously.

The std::async returns a std::future that can be used to wait for the result (or, in the case of the deferred launch policy, evaluate the callable).
https://compiler-explorer.com/z/MdhG33E6n

```c++

int main() {
    // Read the content of a file into a std::vector
    auto read_file = [](std::filesystem::path path) {
        std::vector<char> content;
        std::ifstream file(absolute(path), file.binary | file.in);
        std::copy(std::istreambuf_iterator<char>{file}, 
            std::istreambuf_iterator<char>{},
            std::back_inserter(content));
        return content;
    };

    // Read a file asynchronously
    auto handle = std::async(std::launch::async, read_file, "./example.cpp");

    /* other operations... */

    // Block until ready
    std::vector<char> data = handle.get();

    std::cout << "data.size() == " << data.size() << "\n";

    int x{1}, y{2};
    // Make a lazy function that evaluates x + y
    auto expr = std::async(std::launch::deferred, [&]{ return x + y; });

    x = 40;
    int result = expr.get();
    // result == 42
    // Note: please don't actually write code like this ;-)

    std::cout << "result == " << result << "\n";
}
```
The C++20 std::views::split and std::views::lazy_split produce a view over subranges obtained by splitting a range using a provided delimiter.

std::views::lazy_split offers lazy processing; however, as a consequence, the modelled range is input/forward only.

std::views::split will maintain the underlying range category.
https://compiler-explorer.com/z/brfjMY4jx

```c++

int main() {
    std::string_view text = "the quick brown fox jumps over the lazy dog";
    for (auto word: text | 
        std::views::lazy_split(' ') | // split by word
        std::views::filter([](auto v) { // filter out "the"
            // equal can operate on input ranges
            return not std::ranges::equal(
                v, std::string_view("the"));
        })) {
        // copy can operate on input ranges
        std::ranges::copy(word,
            std::ostream_iterator<char>(std::cout, ""));
        std::cout << ", ";
    }
    std::cout << '\n';
    // prints: quick, brown, fox, jumps, over, lazy, dog,

    std::string_view ip_addr = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
    std::array<uint16_t, 8> parsed;
    std::ranges::copy(
        ip_addr | 
        std::views::split(':') | // split by ':'
        std::views::transform([](auto v) -> uint16_t { // parse
            uint16_t result = 0;
            // can't use views::lazy_split
            // from_chars requires a contiguous range
            std::from_chars(v.data(), v.data()+v.size(), result, 16);
            return result;
        }),
        parsed.begin());
    // parsed == {0x2001, 0xdb8, 0x85a3, 0x0, 0x0, 0x8a2e, 0x370, 0x7334}

    for (uint16_t hunk : parsed)
        std::print("{:0>4x} ", hunk);
    std::println("");
}
```
The std::rotate_copy algorithm will write a rotated version of the input range to the provided output iterator.

A range rotated around a pivot is the subrange [pivot, end) followed by [begin, pivot).

The algorithm does have a parallel variant.
https://compiler-explorer.com/z/sczodeEYo

```c++

int main() {
    std::vector<int> data{1,2,3,4,5,6,7,8,9};
    std::vector<int> out;

    // The element pointed to by the pivot will be the new first element
    auto pivot = data.begin() + 2;
    // *pivot == 3

    std::rotate_copy(
        data.begin(), pivot, data.end(),
        std::back_inserter(out));
    // out == {3, 4, 5, 6, 7, 8, 9, 1, 2}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << '\n';

    auto pivot2 = data.begin() + 5;
    // *pivot2 == 6

    // Range version
    std::ranges::rotate_copy(data, pivot2, out.begin());
    // out == {6, 7, 8, 9, 1, 2, 3, 4, 5}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << '\n';

    // Parallel version
    std::rotate_copy(std::execution::par_unseq,
        data.begin(), pivot, data.end(),
        out.begin());
    // out == {3, 4, 5, 6, 7, 8, 9, 1, 2}

    for (auto v : out)
        std::cout << v << " ";
    std::cout << '\n';
}
```
When using the C++20 format library to format text into a buffer:

- without relying on std::vector and std::back_inserter<br />- without truncating the output using std::format_to_n

We have to first calculate the length of the formatted text using std::formatted_size.
https://compiler-explorer.com/z/1dqee1qKz

```c++

int main() {
    using namespace std::numbers;

    // calculate the required size to store the formatted text
    size_t sz = std::formatted_size("pi == {}", pi_v<double>);

    // allocate a big enough buffer (+ 1 for '\0')
    auto buffer = std::make_unique_for_overwrite<char[]>(sz+1);
    
    // format text into the buffer
    std::format_to(buffer.get(), "pi == {}", pi_v<double>);
    buffer[sz] = '\0'; // terminate the string

    // buffer.get() == "pi == 3.141592653589793"

    std::cout << buffer.get() << "\n";
}
```
The standard library provides two unordered containers: std::unordered_map and std::unordered_set (and their multi_ variants that allow multiple instances of the same key).

These containers provide average O(1) complexity for find, insert and erase operations.

Both containers are node-based, typically providing worse performance than 3rd party flat-hash-map implementations.
https://compiler-explorer.com/z/1sdh7YEx7

```c++

// Custom keys need to provide a hash specialization.
struct Key {
    uint64_t id;
    std::string label;
    // Besides a hash specialization, we need to provide operator==
    friend bool operator==(const Key&, const Key&) = default;
};

// Specialization of hash for a custom type:
template <> struct std::hash<Key> {
    std::size_t operator()(const Key& key) const noexcept {
        std::size_t h1 = std::hash<uint64_t>{}(key.id);
        std::size_t h2 = std::hash<std::string>{}(key.label);
        return h1 ^ (h2 << 1); // or use boost::hash_combine
    }
};

int main() {
    std::unordered_map<uint64_t, std::string> data;

    // insert new element if key doesn't exist
    data.insert(std::make_pair(UINT64_C(0), std::string("dog")));
    // data == {{0, "dog"}}
    std::println("data == {}", data);
    
    // C++17: insert if key doesn't exist, 
    // update value if key already exists
    data.insert_or_assign(UINT64_C(1), std::string("cat"));
    // data == {{0, "dog"}, {1, "cat"}}
    std::println("data == {}", data);

    // C++17: if key doesn't exist, insert a new element,
    // constructing the value in-place from the arguments
    data.try_emplace(UINT64_C(1), "monkey"); // 1, 2 used for the value
    // data == {{0, "dog"}, {1, "cat"}}
    std::println("data == {}", data);

    auto it1 = data.find(0); // lookup by key
    // it1->first == 0, it1->second == "dog"
    std::println("it1->first == {}, it1->second == {}", it1->first, it1->second);

    auto it2 = data.find(4);
    // it2 == data.end()
    std::println("(it2 == data.end()) == {}", it2 == data.end());

    // iterate over elements in unspecified order
    for (auto& [key, value] : data) {
        std::println("key == {}, value == {}", key, value);
    }

    std::unordered_set<Key> set{{0, "label1"}, {0, "label2"},
                                {1, "label1"}, {1, "label2"}};
    bool check = set.contains({0, "label1"});
    // check == true
    std::println("check == {}", check);
}
```
C++17 introduced two simple constants that can be used to prevent false sharing or to ensure true sharing.

The destructive size specifies the minimum offset between two objects to prevent false sharing, and the constructive size specifies the maximum size of continuous memory capable of true sharing.
https://compiler-explorer.com/z/vvqrKjxa1

```c++

struct NoShare {
  // Ensures that flag1 and flag2 do not share a (L1) cache line
  alignas(std::hardware_destructive_interference_size) 
    std::atomic<bool> flag1;
  alignas(std::hardware_destructive_interference_size) 
    std::atomic<bool> flag2;
};

struct Shared {
    std::atomic<bool> flag;
    int data;
    int also_data;
};
// Ensure that flag, data and other_data will be in the same cache line
static_assert(sizeof(Shared) <= std::hardware_constructive_interference_size);

int main() {
    // Align using destructive size to prevent false sharing
    // with other local variables
    alignas(sizeof(std::hardware_destructive_interference_size))
        Shared shared;
}
```
The std::bind can be used to arbitrarily bind and remap arguments to (potentially multiple) callable objects.

Note that std::bind_front (C++20) and std::bind_back (C++26) offer better performance when you only need to bind leading or trailing arguments.

For complex use cases, a lambda might also offer better readability.
https://compiler-explorer.com/z/W7hPMbsnb

```c++

int main() {
    // The constructed object stores the callable 
    // and bound arguments
    auto fill = [](int &x) { x = 42; };
    auto b1 = std::bind(fill, 10);
    b1(); // OK
    // The bound argument stored in b1 now has value 42

    int v = 0;
    auto b2 = std::bind(fill, v);
    b2(); // OK, but no effect
    // v == 0
    // The bound argument stored in b2 now has value 42

    std::cout << "v == " << v << '\n';

    auto b3 = std::bind(fill, std::ref(v));
    b3(); // OK, b3 stores a reference to v
    // v == 42

    std::cout << "v == " << v << '\n';

    auto inc_print = [](int &x) { std::cout << x++ << '\n'; };
    auto b4 = std::bind(inc_print, 42);
    b4(); // prints 42
    b4(); // prints 43
    b4(); // prints 44
    
    // Placeholders (ordinals/1-indexed) represent arguments 
    // of the constructed object
    auto fn1 = [](int a, int b, int c) { return (a+b)/c; };

    using namespace std::placeholders;
    auto b5 = std::bind(fn1, _1, _2, _3);
    int r1 = b5(1, 2, 3);
    // r1 == (1+2)/3 == 1

    std::cout << "r1 == " << r1 << '\n';

    auto b6 = std::bind(fn1, _3, _1, _2);
    int r2 = b6(1, 2, 3);
    // r2 == (3+1)/2 == 2

    std::cout << "r2 == " << r2 << '\n';

    // Placeholders are shared within nested bind expressions
    auto fn = std::bind(
        std::plus<>{}, 
        _1, std::bind(std::multiplies<>{}, _1, _2)
    );
    // Same as:
    auto cl = [](auto&& a, auto&& b) {
        return std::plus<>{}(
            std::forward<decltype(a)>(a),
            std::multiplies<>{}(
                std::forward<decltype(a)>(a), 
                std::forward<decltype(b)>(b)));
    };
    // fn(2,3) == cl(2,3) == 2+2*3

    std::cout << "fn(2,3) == " << fn(2,3) << '\n';
    std::cout << "cl(2,3) == " << cl(2,3) << '\n';

    // Excess arguments are discarded (still evaluated)
    auto b7 = std::bind([]{});
    b7(10, 20, 30, std::cout << "discarded\n"); // OK
    // prints "discarded"
}
```
When we use std::span, we typically use its dynamically sized variant.

However, std::span also supports static sizing, allowing it to maintain the performance of a compile-time known size.

A dynamically sized std::span can be constructed from any contiguous range, while a statically sized std::span can only be constructed from a statically sized contiguous range.
https://compiler-explorer.com/z/97aY3bnK8

```c++

size_t fn1(std::span<int> data) {
    size_t result = 0;
    for (size_t i = 0; i < data.size(); ++i)
        result += i;
    return result;
}

size_t fn2(std::span<int, 1024> data) {
    size_t result = 0;
    for (size_t i = 0; i < data.size(); ++i)
        result += i;
    return result;
}

int main() {
    std::array<int, 1024> data1;
    std::span arr1 = data1;
    // decltype(arr1) == std::span<int, 1024>

    static_assert(std::is_same_v<decltype(arr1), std::span<int, 1024>>);

    int data2[1024];
    std::span arr2 = data2;
    // delctype(arr2) == std::span<int, 1024>

    static_assert(std::is_same_v<decltype(arr2), std::span<int, 1024>>);

    std::vector<int> data3(16);
    fn1(data3); // OK
    // fn2(data3); // Wouldn't compile
}
```
The std::complex is a type template for storing complex numbers.

The template is guaranteed to be specialized for float, double and long double; however, the standard library implementations can offer additional specializations.
https://compiler-explorer.com/z/6Ydejcaqb

```c++

int main(){
    // Complex numbers are specialized for float, double and long double
    std::complex<float> bottom_left{-2, -1.12};
    std::complex<float> top_right{0.7, 1.12};

    // real() and imag() methods to access the corresponding components
    for (float i = bottom_left.imag(); i < top_right.imag(); i+= 0.06) {
        for (float r = bottom_left.real(); r < top_right.real(); r+= 0.025) {

            std::complex<float> c{r,i};
            size_t iter = 0;
            // math functions
            for (std::complex<float> z=c; iter < 26 && abs(z) < 2; ++iter)
                z = z*z + c; // standard arithmetic operations

            // Print the mandelbrot set value for this "pixel"
            std::cout << static_cast<char>(iter+32);
        }
        std::cout << '\n';
    }
}
```
std::inclusive_scan and std::exclusive_scan scan are std::reduce variants that emit each partial result instead of producing a single value.

For inclusive_scan, the first output value already includes the first input element; for exclusive_scan, the first output value is the init value.

Currently, there is no corresponding range algorithm.
https://compiler-explorer.com/z/7cfsxoe9M

```c++

int main() {
    std::vector<int> data{2, 3, 4, 5, 6, 7};

    std::vector<int> incl;
    std::inclusive_scan(data.begin(), data.end(),
                        std::back_inserter(incl));
    // implicit init == int{} == 0
    // incl == {2, 5, 9, 14, 20, 27}

    std::println("incl == {}", incl);

    // Same as std::reduce, if the operation isn't associative,
    // the results will be non-deterministic
    std::inclusive_scan(data.begin(), data.end(), 
        incl.begin(), std::plus<>{}, 100);
    // incl == {102, 105, 109, 114, 120, 127}

    std::println("incl == {}", incl);

    std::vector<int> excl;
    std::exclusive_scan(data.begin(), data.end(), 
                        std::back_inserter(excl), 1);
    // init is mandatory for exclusive_scan
    // excl == {1, 3, 6, 10, 15, 21}

    std::println("excl == {}", excl);

    std::vector<int> product;
    std::exclusive_scan(data.begin(), data.end(), 
        std::back_inserter(product),
        1, std::multiplies<>{});
    // product == {1, 2, 6, 24, 120, 720}

    std::println("product == {}", product);
}
```
Type templates, and since C++14, variable templates can be partially specialized.

A partial specialization can specify or constrain arguments of the base template and provide a custom implementation.

When a template is instantiated, the most specific matching specialization is selected.
https://compiler-explorer.com/z/s64P1c1hG

```c++
template <typename T, typename U>
struct Type {};

// The specialization has to specialize something
template <typename X>
struct Type<int, X> {};

/* Wouldn't compile, doesn't specialize
template <typename X, typename Y>
struct Type<X,Y> {};
*/

/* Default arguments in specializations are not permitted
template <typename X = int>
struct Type<X, X> {};
*/

template <typename Type, Type Value>
struct NonType {};

// Dependent arguments cannot be freely specialized
template <>
struct NonType<int, 4> {}; // OK

/* Wouldn't compile
template <typename X>
struct NonType<X, 4> {};
*/

// The most specific specialization will be selected
template <typename T, typename U>
struct Ordering { static constexpr int v = 0; };

template <typename T, typename U>
struct Ordering<T*, U*> { static constexpr int v = 1; };

template <>
struct Ordering<int*, double*> { static constexpr int v = 2; };


// Also works for variable templates
template <typename X, typename Y>
constexpr inline std::size_t ID = 0;

template <typename Y>
constexpr inline std::size_t ID<Y, int> = 10;

int main() {
    Ordering<int, double> x;      // base template
    Ordering<double*, double*> y; // Ordering<T*,U*>
    Ordering<int*, double*> z;    // Ordering<int*, double*>

    static_assert(x.v == 0);
    static_assert(y.v == 1);
    static_assert(z.v == 2);
}
```
When enabling formatting for custom types through std::format and std::print, we have to specify not only how to print our type but also how to parse the format string.

While printing is a runtime operation, format parsing is done during compile-time, requiring a constexpr implementation.
https://compiler-explorer.com/z/nYzW1W1P3

```c++

struct CustomObject {};

// Specialization for the formatter type:
template <> struct std::formatter<CustomObject> {
    // We do not parse anything, but we still need to advance 
    // the iterator over the corresponding {} in the format.
    // This happens at compile-time.
    constexpr auto parse(std::format_parse_context& ctx) {
        auto it = ctx.begin();
        while (it != ctx.end() && *it != '}') 
            ++it;
        return it;
    }
    // Runtime formatting, we simply add "CustomObject" to the buffer
    auto format(const CustomObject&, auto& ctx) const {
        return std::format_to(ctx.out(), "CustomObject");
    }
};

struct Wrapper {
    int value;
};

// Since our wrapper is effectively an int
// we can inherit from the int formatter.
template <typename CharT>
struct std::formatter<Wrapper, CharT> : std::formatter<int, CharT> {  
    // parse() is inherited, we need a format method matching our type
    auto format(const Wrapper& v, auto& ctx) const {
        return std::formatter<int, CharT>::format(v.value, ctx);
    }
};

struct Greeter {};

// Custom format example: {}, {:u} or {:w}
template <> struct std::formatter<Greeter> {
    void unexpected_format_specification_for_greeter(){}
    constexpr auto parse(std::format_parse_context& ctx) {
        auto it = ctx.begin();
        while (it != ctx.end() && *it != '}') {
            switch (*it) {
                case 'w': object = "World"; break;
                case 'u': object = "Universe"; break;
                // we call a non-constexpr function to trigger an error
                default: unexpected_format_specification_for_greeter();
            }
            ++it;
        }
        return it;
    }
    auto format(const Greeter&, auto& ctx) const {
        return std::format_to(ctx.out(), "Hello {}!", object);
    }
    std::string_view object = "World";
};

int main() {
    auto str1 = std::format("{}, {}", CustomObject{}, CustomObject{});
    // str1 == "CustomObject, CustomObject"

    std::println("{}, {}", CustomObject{}, CustomObject{});

    auto str2 = std::format("0x{:X}", Wrapper{255});
    // str2 == "0xFF"

    std::println("0x{:X}", Wrapper{255});

    auto str3 = std::format("{:u}", Greeter{});
    // str3 == "Hello Universe!"

    std::println("{:w}", Greeter{});
    std::println("{:u}", Greeter{});

    // std::format("{:Q}", Greeter{}); // Wouldn't compile:
    // call to non-'constexpr' function
    // ...unexpected_format_specification_for_greeter()
}
```
If you need to format values into a std::string or extract formatted values from a std::string and are stuck in the pre-C++20 world, your main option is std::stringstream.

std::istringstream can be filled with a string and treated as any other istream object.

std::ostringstream can be treated as an ostream object with the option to extract the current content as std::string.
https://compiler-explorer.com/z/rYxYaxn37

```c++

int main() {
    std::istringstream in("10.17 22.3 \"Hello World\" 1234");
    float f1{}, f2{};
    std::string s;
    int i{};

    in >> f1 >> f2 >> std::quoted(s) >> i;
    // f1 == 10.17, f2 == 22.3, s == "Hello World", i == 1234

    std::cout << "f1 == " << f1 << ", f2 == " << f2 << ", s == " << std::quoted(s) << ", i == " << i << '\n';

    std::ostringstream out;

    out << f1 << " " << f2 << " " << std::quoted(s) << " " << i;
    auto str1 = out.str();
    // decltype(str1) == std::string
    // str1 == "10.17 22.3 \"Hello World\" 1234"

    static_assert(std::is_same_v<decltype(str1), std::string>);
    std::cout << "str1 == " << std::quoted(str1) << '\n';
    
    auto str2 = out.view(); // C++20
    // decltype(str2) == std::string_view
    // str2 == "10.17 22.3 \"Hello World\" 1234"

    static_assert(std::is_same_v<decltype(str2), std::string_view>);
    std::cout << "str2 == " << std::quoted(str2) << '\n';
}
```
When working with exceptions in C++, there are three main levels of guarantees a function can provide when an exception is thrown.

🧵👇
https://compiler-explorer.com/z/T9YKKa5oW

```c++

// A custom type that can throw on copy as requested
struct TroubleMaker {
    TroubleMaker() {}
    TroubleMaker(const TroubleMaker&) {
        if (now_ > 0) --now_;
        if (now_ == 0) throw std::runtime_error("I'm failing");
    }
    auto operator<=>(const TroubleMaker&) const = default;
    static int now_;
};

int TroubleMaker::now_ = -1;

int main() {
    std::vector<TroubleMaker> data{{}, {}, {}, {}, {}};

    std::vector<TroubleMaker> strong;
    try {
        // Throw an exception when copying the 3rd element
        TroubleMaker::now_ = 3;
        // std::vector provides strong exception guarantee
        strong.insert(strong.end(), data.begin(), data.end());
        // Note: technically this specific combination isn't strong 
        // guarantee as described by the standard, but both GCC and 
        // Clang implement it as strong guarantee
    } catch (...) {
        // strong.size() == 0
        // because an exception was thrown, state was not changed
        std::cerr << "strong.size() == " << strong.size() << '\n';
    }

    std::multiset<TroubleMaker> weak;
    try {
        // Throw an exception when copying the 3rd element
        TroubleMaker::now_ = 3;
        // map/set provide basic exception guarantee
        weak.insert(data.begin(), data.end());
    } catch (...) {
        // weak.size() == 2
        // invariants are maintained, resources are not leaked
        // but the operation partially succeeded
        std::cerr << "weak.size() == " << weak.size() << '\n';
    }
}
```
C++23 formalized the concepts of tuple-like and pair-like types.

The set of tuple-like types is currently: std::array, std::pair, std::tuple, std::ranges::subrange and std::complex (since C++26).

All tuple-like types support a base set of operations:

• assignment<br />• comparisons<br />• std::apply, std::make_from_tuple<br />• std::tuple_size, std::tuple_element<br />• std::tuple_cat
https://compiler-explorer.com/z/KMTYnPY34

```c++


int main() {
    auto a = std::tuple{1, 3.4, "Hello World"};
    auto b = std::pair{7, 1.7};
    auto c = std::array{1,2,3};
    auto d = std::tuple{9,8,7};

    // Comparison
    // c < d == true
    std::println("c < d == {}", c < d);

    // Assignment
    d = c; // array -> tuple
    // d == {1,2,3}

    std::println("d == [{},{},{}]", std::get<0>(d), std::get<1>(d), std::get<2>(d));

    auto fn = [](int a, double b) { return a + b; };

    // std::apply: call a function with the elements 
    // of the tuple-like object as arguments
    auto r = std::apply(fn, b);
    // decltype(r) == double, r == 8.7

    std::println("r == {}", r);

    // std::make_from_tuple: construct a object with the elements
    // of the tuple-like object as arguments of the constructor
    struct X { int x; int y; int z; };
    auto obj = std::make_from_tuple<X>(c);
    // obj == {1, 2, 3}

    std::println("obj == {{{},{}, {}}}", obj.x, obj.y, obj.z);

    // std::tuple_element: the i-th element type
    using e_t = std::tuple_element_t<1, decltype(a)>;
    // e_t == double

    static_assert(std::is_same_v<e_t,double>);

    // std::tuple_size: number of elements
    size_t sz = std::tuple_size<decltype(b)>{};
    // sz == 2

    std::println("sz == {}", sz);

    // std::tuple_cat:
    // concatenate two tuple-like objects into a tuple
    auto cat = std::tuple_cat(b, c);
    // decltype(cat) == std::tuple<int, double, int, int, int>
    // cat == {7, 1.7, 1, 2, 3}

    std::println("cat == [{},{},{},{},{}]", 
        std::get<0>(cat), std::get<1>(cat), std::get<2>(cat),
        std::get<3>(cat), std::get<4>(cat));
}
```
When calling a member function, the name lookup can lead to surprising behaviour.

Name lookup operates in steps and will stop when it encounters any match; this includes non-viable matches.

The lookup always starts in the class type scope that matches the variable&#39;s static type, not the dynamic type of the object.
https://compiler-explorer.com/z/W95eoe1oP

```c++

struct Base {
    void fn() { std::cout << "Base::fn()\n"; }
    void fn(int) { std::cout << "Base::fn(int)\n"; }
    void call() { fn(); }
};

struct Derived : Base {
    void fn() { std::cout << "Derived::fn\n"; }
};

template <typename T>
struct MixinA {
    void fn() { std::cout << "MixinA::fn()\n"; }
    void call() {
        static_cast<T*>(this)->fn();
    }
};

struct MixinB {
    void fn() { std::cout << "MixinB::fn()\n"; }
    void call(this auto& self) {
        self.fn();
    }
};

struct UseA : MixinA<UseA> {
    void fn() { std::cout << "UseA::fn()\n"; }
};

struct UseB : MixinB {
    void fn() { std::cout << "UseB::fn()\n"; }
};

struct SideNote {
    void fn() {}
};

struct DerivedNote : SideNote {
    int fn;
};
 
int main() {
    Base base;
    base.fn();   // Calls Base::fn()
    base.fn(42); // Calls Base::fn(int)

    std::cout << "--\n";

    Derived derived;
    derived.fn(); // Calls Derived::fn()
    // derived.fn(42); // Will not compile, lookup finds Derived::fn()
                    // which isn't viable
    derived.call(); // Calls Base::call() -> Base::fn()
                    // 'this' in Base::call() is Base*

    std::cout << "--\n";

    // The lookup is based on the type of the variable,
    // not the dynamic type of the object
    Base& ref = derived;
    ref.fn(42); // Calls Base::fn(int)

    Base* ptr = &derived;
    ptr->fn(); // Calls Base::fn()

    // Same logic with member function pointers
    void (Base::*fn_ptr)(int) = &Base::fn;
    (derived.*fn_ptr)(42); // Calls Base::fn(int)

    std::cout << "--\n";

    UseA a;
    a.call(); // Calls MixinA::call() -> UseA::fn()

    UseB b;
    b.call(); // Calls MixinB::call() -> UseB::fn()

    // However, we are still dealing with the static type
    MixinB& mb = b;
    mb.call(); // Calls MixinB::call() -> MixinB::fn()
}
```
Virtual member functions modify the behaviour of calling member functions.

During object construction, the final overrider of each virtual member function is determined and stored in a table (vtable).

If the name lookup finds a virtual member function, it will insert an indirection through the vtable instead of calling it directly.
https://compiler-explorer.com/z/GaMz74nKW

```c++

struct Base {
    virtual void fun() const { std::cout << "Base::fun()\n"; }
    virtual ~Base() = default; // If a class has at least one virtual
                               // member function, it should have 
                               // a virtual (or private) destructor
};

struct Derived : Base {
    void fun() const override { std::cout << "Derived::fun()\n"; }
    // void fun() override {} // Wouldn't compile, cv-qualifiers do not match
    ~Derived() override = default;
};

struct Abstract {
  	// Pure virtual member function, the derived classes must provide
    // an overriding implementation
    virtual void fun() const = 0;
    virtual ~Abstract() = default;
};

// Despite the method being 
void Abstract::fun() const { std::cout << "Abstract::fun()\n"; }

struct Concrete : Abstract {
    void fun() const override { std::cout << "Concrete::fun()\n"; }
    ~Concrete() override = default;
};

struct Middle : Base {
    void fun(int) const { std::cout << "Middle::fun(int)\n"; }
    ~Middle() override = default;
};

struct Final : Middle {
    void fun() const override { std::cout << "Final::fun()\n"; }
    ~Final() override = default;
};

int main() {
    Base base;
    base.fun(); // Calls Base::fun()

    Derived derived;
    derived.fun();       // Calls Derived::fun()
    derived.Base::fun(); // Calls Base::fun() explicitly

    Base& ref = derived;
    ref.fun(); // fun is virtual, actual type is Derived, 
               // final overrider is Derived::fun()
    ref.Base::fun(); // Calls Base::fun() explicitly

    std::cout << "--\n";

    // Abstract abstract; // Wouldn't compile, abstract class

    Concrete concrete;
    concrete.fun();
    concrete.Abstract::fun();

    Abstract* aref = &concrete;
    aref->fun();

    std::cout << "--\n";

    Middle mid;
    // mid.fun(); // Wouldn't compile
    mid.fun(42); // Calls Middle::fun(int)

    const Base& fin = Final{};
    fin.fun(); // Calls Final::fun()

    const Middle& hmm = Final{};
    // hmm.fun(); // Wouldn't compile, Base::fun() is hidden
    hmm.Base::fun(); // Calls Base::fun() explicitly
    static_cast<const Base&>(hmm).fun(); // Calls Final::fun()
}
```
If you interact with legacy or highly portable APIs, you will often encounter various types of handles represented by trivial types.

In C++, we want to wrap these handles in an RAII wrapper to ensure we do not leak resources or access them after they are gone. However, this means a lot of boilerplate code.

Fortunately, this process can be simplified with a re-usable Mixin.
https://compiler-explorer.com/z/9YP7sKP1q

```c++

enum class Style : bool { Implicit, Explicit };

template <typename T, T empty_value = T{}, 
          Style style = Style::Explicit, auto cleanup = [](T){}>
struct MoveOnly {
    static constexpr bool is_explicit = (style == Style::Explicit);

    // Default construction
    MoveOnly() : store_(empty_value) {}
    // Constructor from the base type
    explicit(is_explicit) MoveOnly(T value) 
        : store_(std::move(value)) {}

    // Move constructor
    MoveOnly(MoveOnly&& other)
        : store_(std::exchange(other.store_, empty_value)) {}
    // Move assignment
    MoveOnly& operator=(MoveOnly&& other) {
        store_ = std::exchange(other.store_, empty_value);
        return *this;
    }

    // Destructor
    ~MoveOnly() {
        cleanup(store_);
    }

    // Conversion to the base type
    [[nodiscard]] explicit(is_explicit) operator T() const {
        return store_;
    }

    // Explicit access to the stored value
    [[nodiscard]] T get() const { return store_; }
    void set(T value) { store_ = std::move(value); }
private:
    template <typename>
    friend struct ingest;
    T store_;
};

// Alternative to var.set(value) for explicit style
// ingest{var} = value;
template <typename T>
struct ingest {
    template <T v, Style s, auto c>
    ingest(MoveOnly<T,v,s,c>& in) : store_(in.store_) {}
    ingest& operator=(T value) {
        store_ = value;
        return *this;
    }
private:
    T& store_;
};

// Example use for a single value, used as a Mixin
struct UnixFile : MoveOnly<int, -1, Style::Implicit, 
                    [](int fd) { if (fd != -1) close(fd); }> {
    UnixFile(const std::filesystem::path& path)
        : MoveOnly(open(path.c_str(), O_RDONLY)) {
        if (*this == -1)
            throw std::runtime_error("Failed to open file.");
    }
};

// Example use for multiple values used as members
struct MemoryMappedFile {
    MemoryMappedFile(const std::filesystem::path& path)
        : file_(path) {
        struct stat sb;
        if (fstat(file_, &sb) == 1)
            throw std::system_error(errno, std::system_category(),
                                    "Failed to read file stats");
        sz_.set(sb.st_size); // Explicit interface

        // begin_.set(...) would be less readable
        ingest{begin_} = static_cast<char *>(
            mmap(nullptr, sz_.get(), PROT_READ, MAP_PRIVATE, file_, 0));
        if (begin_.get() == MAP_FAILED)
            throw std::system_error(errno, std::system_category(),
                                    "Failed to map file to memory");
    }

    // We still have to declare move constructor/assignment
    // disabled by providing a custom destructor
    MemoryMappedFile(MemoryMappedFile&&) = default;
    MemoryMappedFile& operator=(MemoryMappedFile&&) = default;

    ~MemoryMappedFile() {
        if (begin_.get() != nullptr)
            munmap(begin_.get(), sz_.get());
    }
    std::span<const char> file_content() {
        assert(begin_.get() != nullptr);
        return {begin_.get(), sz_.get()};
    }
private:
    UnixFile file_;
    MoveOnly<char *> begin_;
    MoveOnly<size_t> sz_;
};


int main() {
    UnixFile f("./example.cpp");
    auto moved = std::move(f);
    std::cout << "./example.cpp opened as file descriptor no. " << (int)moved << "\n";
    std::cout << "moved from state: " << (int)f << "\n";

    MemoryMappedFile mf("./example.cpp");
    auto mmap_moved = std::move(mf);

    std::cout << "first 20 bytes of ./example.cpp:\n";
    for (char c : mmap_moved.file_content().subspan(0, 20))
        std::cout << c;
    std::cout << '\n';
}
```
The std::to_chars is a low-level tool for formatting integer and floating-point values as text into a buffer.

The integer overload supports bases up to 35 (10..35 represented using lowercase characters a-z).

The overloads for floating-point values allow specification of the format and precision. Format specification mirrors the behaviour of the printf function.
https://compiler-explorer.com/z/qq93ff6K4

```c++

int main() {
    char buffer[5];
    {
    // Formatting integers
    auto [end, err] = std::to_chars(buffer, buffer+5, 12345);
    // [buffer, end) == "12345"
    std::cout << std::string_view(buffer, end) << '\n';
    }
    {
    // With a custom base
    auto [end, err] = std::to_chars(buffer, buffer+5, 12345, 35);
    // [buffer, end) == "a2p"
    std::cout << std::string_view(buffer, end) << '\n';
    }
    {
    // If the formatted values doesn't fit the buffer
    // error is returned and the buffer is left in unspecified state
    auto [end, err] = std::to_chars(buffer, buffer+5, 12345, 2);
    // std::make_error_code(err).message() == "Value too large for defined data type"
    if (err != std::errc{})
        std::cout << "Failed to format: " << std::make_error_code(err).message() << '\n';
    }
    {
    // Formatting floating-point
    auto [end, err] = std::to_chars(buffer, buffer+5, 3.14);
    // [buffer, end) == "3.14"
    std::cout << std::string_view(buffer, end) << '\n';
    }
    {
    // With custom format and precision
    auto [end, err] = std::to_chars(buffer, buffer+5, std::numbers::pi, std::chars_format::fixed, 3);
    // [buffer, end) == "3.142"
    std::cout << std::string_view(buffer, end) << '\n';
    }

    // Different floating point formats
    char buf[1024];
    {
    auto [end, _] = std::to_chars(buf, buf+1024, std::numbers::pi * 1000, std::chars_format::scientific);
    // [buf, end) == 3.141592653589793e+03
    std::cout << std::string_view(buf, end) << '\n';
    }
    {
    auto [end, _] = std::to_chars(buf, buf+1024, std::numbers::pi * 1000, std::chars_format::fixed);
    // [buf, end) == 3141.592653589793
    std::cout << std::string_view(buf, end) << '\n';
    }
    {
    auto [end, _] = std::to_chars(buf, buf+1024, std::numbers::pi * 1000, std::chars_format::hex);
    // [buf, end) == 1.88b2f704a9409p+11
    std::cout << std::string_view(buf, end) << '\n';
    }
    {
    // General switches between fixed and specientific
    auto [end, _] = std::to_chars(buf, buf+1024, std::numbers::pi * 1000, std::chars_format::general);
    // [buf, end) == 3141.592653589793
    std::cout << std::string_view(buf, end) << '\n';
    }
    {
    auto [end, _] = std::to_chars(buf, buf+1024, std::numbers::pi * 1000, std::chars_format::general, 3);
    // [buf, end) == 3.14e+03
    std::cout << std::string_view(buf, end) << '\n';
    }
}
```
When working with virtual functions, we can encounter situations where we need to return a different type from an overriding function.

While this isn&#39;t possible in the general case, the types returned are allowed to be different if they are covariant (the overriding function returns a derived type).

The prototypical use for covariant return types is a clone() function.
https://compiler-explorer.com/z/qTxErsxbE

```c++

struct Base {
    virtual Base* clone() const {
        std::cout << "Base::clone()\n";
        return new Base(*this);
    }
    virtual ~Base() = default;
};

struct Derived : Base {
    Derived* clone() const override {
        std::cout << "Derived::clone()\n";
        return new Derived(*this);
    }
    ~Derived() override = default;
};

// Because covariant types require a raw reference or pointer, 
// we can't make smart pointers covariant
struct BaseSmart {
    virtual ~BaseSmart() = default;

    // A clone wrapper for the base type.
    // If you also want std::unique_ptr<DerivedSmart> the solutions get lot more complicated.
    friend std::unique_ptr<BaseSmart> clone(const std::unique_ptr<BaseSmart>& src) {
        return std::unique_ptr<BaseSmart>(src->clone_impl());
    }
private:
    virtual BaseSmart* clone_impl() const {
        std::cout << "BaseSmart::clone_impl()\n";
        return new BaseSmart(*this);
    }
};

struct DerivedSmart : BaseSmart {
    ~DerivedSmart() override = default;
private:
    DerivedSmart* clone_impl() const override {
        std::cout << "DerivedSmart::clone_impl()\n";
        return new DerivedSmart(*this);
    }
};

Base* get_object() { return new Derived(); }
std::unique_ptr<BaseSmart> get_smart() { return std::unique_ptr<BaseSmart>{new DerivedSmart()}; }

int main() {
    Base* p1 = get_object();
    Base* p2 = p1->clone(); // Calls Derived::clone()
    delete p1;
    delete p2;

    std::unique_ptr<BaseSmart> p3 = get_smart();
    std::unique_ptr<BaseSmart> p4 = clone(p3); // Calls DerivedSmart::clone_impl
}
```
The four function objects std::bit_and, std::bit_or, std::bit_xor and std::bit_not model the functionality of the corresponding bit operators &amp;, |, ^, ~.

As with all other function objects from the &lt;functional&gt; header, the void specialization will deduce the type from the arguments.
https://compiler-explorer.com/z/5a8xrE9se

```c++

int main() {
    auto v1 = std::bit_and<unsigned>{}(255, 63);
    // decltype(v1) == unsigned, v1 == 63

    static_assert(std::is_same_v<decltype(v1), unsigned>);
    std::println("std::bit_and<unsigned>{{}}(255, 63) == {}", v1);

    auto v2 = std::bit_or<unsigned>{}(255, 63);
    // decltype(v2) == unsigned, v2 == 255

    static_assert(std::is_same_v<decltype(v2), unsigned>);
    std::println("std::bit_or<unsigned>{{}}(255, 63) == {}", v2);

    auto v3 = std::bit_xor<unsigned>{}(255, 63);
    // decltype(v3) == unsigned, v3 == 192

    static_assert(std::is_same_v<decltype(v3), unsigned>);
    std::println("std::bit_xor<unsigned>{{}}(255, 63) == {}", v3);

    auto v4 = std::bit_not<unsigned>{}(0);
    // decltype(v4) == unsigned, v4 == UINT_MAX

    static_assert(std::is_same_v<decltype(v4), unsigned>);
    std::println("std::bit_not<unsigned>{{}}(0) == {}", v4);

    // Deduced version
    auto v5 = std::bit_and<>{}(-1, -31);
    // decltype(v5) == int, v5 == -31

    static_assert(std::is_same_v<decltype(v5), int>);
    std::println("std::bit_and<>{{}}(-1, -31) == {}", v5);
}
```
While the std::shared_ptr has limited use in single-threaded code, in multi-threaded code, the notion of shared ownership is fairly common.

However, operations on a single instance of a std::shared_ptr are not thread-safe.

C++20 introduced std::atomic&lt;std::shared_ptr&lt;T&gt;&gt; and std::atomic&lt;std::weak_ptr&lt;T&gt;&gt;, which can be used to implement thread-safe shared data structures.
https://compiler-explorer.com/z/v9r1vhP83

```c++

// Example of a simple thread-safe 
// and resource-safe stack datastructure
template <typename T>
struct Stack {
    struct Node {
        T value;
        std::shared_ptr<Node> prev;
    };

    void push(T value) {
        // Make a new node, setting the current head as its previous value
        auto active = std::make_shared<Node>(std::move(value), head_.load());

        // Another thread can come in a modify the current head,
        // so check if active->prev is still head_
        // - if it's not, update active->prev to the current value of head_ and loop
        // - if it is, update head_ to active
        while (not head_.compare_exchange_weak(active->prev, active));
    }

    std::optional<T> pop() {
        // Load the current head
        auto active = head_.load();

        // Another thread can come in and modify the current head,
        // so check if the head has changed (i.e. head_ != active)
        // - if it has changed, update active to the current head and loop
        // - if it hasn't changed, update the head to the previous element on the stack
        while (active != nullptr && not head_.compare_exchange_weak(active, active->prev));

        // If we didn't run out of elements, return the value
        if (active != nullptr) return {std::move(active->value)};
        else return std::nullopt;
    }

private:
    std::atomic<std::shared_ptr<Node>> head_;
};


int main() {
    Stack<int> stack;
    std::array<std::jthread, 4> writers;
    std::array<std::jthread, 4> readers;

    // start the writers
    for (auto& t : writers)
        t = std::jthread{[&stack]{ 
            // write 100 values
            for (auto _ : std::views::repeat(true, 100))
                stack.push(42);
        }};
    // start the readers
    for (auto& t : readers)
        t = std::jthread{[&stack]{ 
            // read 100 values
            for (auto _ : std::views::repeat(true, 100))
                while (stack.pop() == std::nullopt);
        }};
}
```
The 𝒎𝒂𝒊𝒏 function is the entry point to a program (in hosted environments) that can have one of the following forms: 

𝒊𝒏𝒕 𝒎𝒂𝒊𝒏() { }<br />𝒊𝒏𝒕 𝒎𝒂𝒊𝒏(𝒊𝒏𝒕 𝒂𝒓𝒈𝒄, 𝒄𝒉𝒂𝒓* 𝒂𝒓𝒈𝒗[]) { }

With a common extension (part of POSIX) that includes environment variables:

𝒊𝒏𝒕 𝒎𝒂𝒊𝒏(𝒊𝒏𝒕 𝒂𝒓𝒈𝒄, 𝒄𝒉𝒂𝒓* 𝒂𝒓𝒈𝒗[], 𝒄𝒉𝒂𝒓* 𝒆𝒏𝒗𝒑[]) { }

🧵👇
https://compiler-explorer.com/z/vPP6oY5bx

```c++
Compiler Explorer is an interactive online compiler which shows the assembly output of compiled C++, Rust, Go (and many more) code.
```
Each object in memory has an alignment imposed on it by the alignment requirement of the corresponding type (the location address must be a multiple).

The main consequence of alignment is that consecutive variables or class members might require additional space for necessary padding.

When constructing objects in place, the pointer may need to be manually aligned.
https://compiler-explorer.com/z/h165WE8n6

```c++

// For x86-64
struct Data { // sizeof == 48
    char a;        // sizeof == 1, alignof == 1
                   // padding 3
    int b;         // sizeof == 4, alignof == 4
                   // padding 8
    long double c; // sizeof == 16, alignof == 16
                   // no padding
    bool d;        // sizeof == 1, alignof == 1
                   // padding 7
    int64_t e;     // sizeof == 8, alignof == 8
};

size_t padding(size_t offset, size_t alignment) {
    if (offset % alignment == 0)
        return 0;
    return alignment - offset % alignment;
}

int main() {
    size_t offset = 0;

    std::println("sizeof(Data) == {} bytes", sizeof(Data));
    std::println("\tsizeof(Data::a): {}", sizeof(Data::a));
    offset += sizeof(Data::a);
    std::println("\tpadding: {}", padding(offset, alignof(decltype(Data::b))));
    offset += padding(offset, alignof(decltype(Data::b)));
    std::println("\tsizeof(Data::b): {}", sizeof(Data::b));
    offset += sizeof(Data::b);
    std::println("\tpadding: {}", padding(offset, alignof(decltype(Data::c))));
    offset += padding(offset, alignof(decltype(Data::c)));
    std::println("\tsizeof(Data::c): {}", sizeof(Data::c));
    offset += sizeof(Data::c);
    std::println("\tpadding: {}", padding(offset, alignof(decltype(Data::d))));
    offset += padding(offset, alignof(decltype(Data::d)));
    std::println("\tsizeof(Data::d): {}", sizeof(Data::d));
    offset += sizeof(Data::d);
    std::println("\tpadding: {}", padding(offset, alignof(decltype(Data::e))));
    offset += padding(offset, alignof(decltype(Data::e)));
    std::println("\tsizeof(Data::d): {}\n", sizeof(Data::e));
    offset += sizeof(Data::e);

    // C++11 std::alignment_of and C++17 std::alignment_of_v
    size_t align_of_int64_t = std::alignment_of<int64_t>::value;

    std::println("align_of_int64_t == {}", align_of_int64_t);

    // same as
    align_of_int64_t = std::alignment_of_v<int64_t>;  // C++17

    std::println("align_of_int64_t == {}\n", align_of_int64_t);

    char bad{}; *(char volatile*)&bad; // inject an anoying offset
    std::byte place1[sizeof(int64_t)]; // not properly aligned for int64_t
    // int64_t *ptr1 = new (place1) int64_t(42); // Undefined Behaviour

    std::println("&place1 % alignof(int64_t) == {}", std::bit_cast<uint64_t>(&place1) % alignof(int64_t));
    
    // Force alignment using alignas (also works for members)
    alignas(int64_t) std::byte place2[sizeof(int64_t)];
    int64_t* ptr2 = new (place2) int64_t(42); // OK
    ptr2->~int64_t();

    std::println("&place2 % alignof(int64_t) == {}\n", std::bit_cast<uint64_t>(&place2) % alignof(int64_t));

    // For pointers, std::align provides a runtime solution
    std::byte place3[32];
    void* ptr3 = place3 + 2; // not properly aligned for int64_t

    std::println("ptr3 % alignof(int64_t) == {}", std::bit_cast<uint64_t>(ptr3) % alignof(int64_t));

    size_t sz = 32-2; // remaining space in place3
    std::align(alignof(int64_t), sizeof(int64_t), ptr3, sz);
    // sz == 32-2-padding
    // ptr3 == place3 + 2 + padding
    
    std::println("place3 == {}, ptr3_aligned == {}, sz == {}", (void*)place3, ptr3, sz);

    // Returns the adjusted pointer or nullptr if the operation is not possible
    void* ptr4 = std::align(16, 17, ptr3, sz);
    // ptr4 == nullptr, can't fit 17 bytes after ptr3 
    // with 16 byte alignment, ptr3 and sz unchanged

    std::println("ptr4 == {}, ptr3 == {}, sz == {}", ptr4, ptr3, sz);

    // std::max_align_t has an alignment requirent at least as strict as scalar types
    alignas(std::max_align_t) char buffer[512]; // typical use
}
```
When handling smart pointers, we might need to change the pointed-to type without changing the pointed-to object.

With std::unique_ptr, this is fairly straightforward. However, if you try to do the same with std::shared_ptr, you need to be careful to maintain the shared ownership.

C++17 simplified this process by introducing four cast-style functions.
https://compiler-explorer.com/z/W6dE6zqzG

```c++

struct File {
    int fd_;
};

struct Base {
    virtual ~Base() = default;
};
struct Derived : Base {
    ~Derived() override = default;
};

int main() {
    // const_cast
    auto ptr = std::make_shared<int>(42);
    // decltype(ptr) == std::shared_ptr<int>
    // ptr.use_count() == 1

    std::println("ptr.use_count() == {}\n", ptr.use_count());
    static_assert(std::is_same_v<decltype(ptr), std::shared_ptr<int>>);

    // A new instance sharing the same ownership,
    // but pointing to const int*
    auto cv1 = std::const_pointer_cast<const int>(ptr); // C++17
    // decltype(cv1) == std::shared_ptr<const int>
    // ptr.use_count() == cv1.use_count() == 2

    std::println("ptr.use_count() == {}, cv1.use_count() == {}\n", ptr.use_count(), cv1.use_count());
    static_assert(std::is_same_v<decltype(cv1), std::shared_ptr<const int>>);

    // Consuming conversion
    auto cv2 = std::const_pointer_cast<int>(std::move(cv1)); // C++20
    // decltype(cv2) == std::shared_ptr<int>
    // ptr.use_count() == 2, cv2.use_count() == 2
    // cv1 == nullptr

    std::println("ptr.use_count() == {}, cv2.use_count() == {}", ptr.use_count(), cv2.use_count());
    std::println("(cv1 == nullptr) == {}\n", cv1 == nullptr);
    static_assert(std::is_same_v<decltype(cv2), std::shared_ptr<int>>);

    // reinterpret_cast
    auto file = std::make_shared<File>(42);
    // decltype(file) == std::shared_ptr<File>

    std::println("file.use_count() == {}\n", file.use_count());
    static_assert(std::is_same_v<decltype(file), std::shared_ptr<File>>);

    auto fd = std::reinterpret_pointer_cast<int>(file);
    // decltype(fd) == std::shared_ptr<int>
    // file.use_count() == fd.use_count() == 2
    // *fd == 42

    std::println("file.use_count() == {}, fd.use_count() == {}", file.use_count(), fd.use_count());
    std::println("*fd == {}\n", *fd);
    static_assert(std::is_same_v<decltype(fd), std::shared_ptr<int>>);

    // static_cast
    auto derived = std::make_shared<Derived>();
    // decltype(derived) == std::shared_ptr<Derived>
    
    static_assert(std::is_same_v<decltype(derived), std::shared_ptr<Derived>>);

    auto base = std::static_pointer_cast<Base>(std::move(derived));
    // decltype(base) == std::shared_ptr<Base>
    // base.use_count() == 1, derived == nullptr

    std::println("base.use_count() == {}", base.use_count());
    std::println("(derived == nullptr) == {}\n", derived == nullptr);
    static_assert(std::is_same_v<decltype(base), std::shared_ptr<Base>>);

    // dynamic_cast
    derived = std::dynamic_pointer_cast<Derived>(base);
    // derived.use_count() == 2, base.use_count() == 2

    std::println("derived.use_count() == {}, base.use_count() == {}", derived.use_count(), base.use_count());
}
```
The std::integer_sequence is a C++14 metaprogramming utility. As its name suggests, it can represent a compile-time sequence of integers.

The underlying integer type can be specified, and the std::index_sequence alias offers a shortcut for std::size_t.

std::make_integer_sequence and std::make_index_sequence are helpers that produce sequences of integers from 0 to N-1.
https://compiler-explorer.com/z/3EKnzG88q

```c++

// C++14 example, reorder elements in a tuple
template <typename Tuple, size_t... I>
constexpr auto shuffle(const Tuple& t, std::index_sequence<I...>) {
    return std::make_tuple(std::get<I>(t)...);
}

// C++17 example, runtime version of std::get
template <size_t idx, typename Tuple, typename T>
bool set_if(const Tuple& t, size_t index, T& out) {
    // if idx == index, set out to the corresponding value
    // and return true
    return (index == idx) && (out = std::get<idx>(t), true);
}

template <typename Tuple, typename T, size_t... I>
void get_impl(const Tuple& t, size_t index, T& out, std::index_sequence<I...>) {
    // linear iteration over all elements of the tuple
    // logical or provides early termination
    (set_if<I>(t, index, out) || ...);
}

template <typename Tuple, typename T>
void get(Tuple& t, size_t index, T& out) {
    get_impl(t, index, out, std::make_index_sequence<std::tuple_size_v<Tuple>>{});
}

// C++20, sort elements of a tuple by size

// Make a sorted list of indices by size
template <typename Tuple, size_t... I>
constexpr auto sort_impl(std::index_sequence<I...>) {
    // Array of indices
    std::array<size_t, sizeof...(I)> idxs{{I...}};
    // Array of element sizes
    std::array<size_t, sizeof...(I)> sizes{{sizeof(std::tuple_element_t<I, Tuple>)...}};
    // Sort the array of indices
    std::ranges::sort(idxs, [&](size_t l, size_t r) {
        return sizes[l] < sizes[r];
    });
    // Return the sorted indices
    return idxs;
}

// Convert an array of size_t into an index sequence
template <auto Arr, std::size_t... I>
constexpr auto to_sequence(std::index_sequence<I...>) {
    return std::index_sequence<Arr[I]...>{};
}

template <typename Tuple>
auto sort(Tuple t) {
    using idxs = std::make_index_sequence<std::tuple_size_v<Tuple>>;
    // We can re-use our C++14 shuffle function
    return shuffle(t, to_sequence<sort_impl<Tuple>(idxs{})>(idxs{}));
}

int main() {
    std::tuple<int, double, bool> t1{42, 3.14, false};
    auto shuffled = shuffle(t1, std::index_sequence<1,2,0>{});
    // decltype(shuffled) == std::tuple<double,bool,int>
    // shuffled == {3.14, false, 42}

    static_assert(std::is_same_v<decltype(shuffled), std::tuple<double,bool,int>>);
    static_assert(shuffle(std::tuple{1,2}, std::index_sequence<1,0>{}) == std::tuple{2,1});

    std::println("shuffled == [{}, {}, {}]", get<0>(shuffled), get<1>(shuffled), get<2>(shuffled));

    std::tuple<int, double, bool> t2{2, 4.2, false};
    int v;
    get(t2, 0, v);
    // v == 2

    std::println("v == {}", v);

    get(t2, 1, v);
    // v == 4

    std::println("v == {}", v);

    get(t2, 2, v);
    // v == 0

    std::println("v == {}", v);

    auto sorted = sort(std::tuple{1,4.2,false});
    // decltype(sorted) == std::tuple<bool,int,double>
    // sorted == {false, 1, 4.2}

    std::println("sorted == [{}, {}, {}]", get<0>(sorted), get<1>(sorted), get<2>(sorted));
    static_assert(std::is_same_v<decltype(sorted), std::tuple<bool,int,double>>);
}
```
While iostreams are mainly known for formatted I/O using stream insertion and extraction operators, they also support unformatted I/O.

• by-character operations: get(), peek(), ungetc(), putback(), put()<br />• delimiter operations: getline(), ignore()<br />• block operations: read(), readsome(), write()<br />• input seeking: tellg(), seekg()<br />• output seeking: tellp(), seekp()
https://compiler-explorer.com/z/jvvTqEeeT

```c++

int main() {
    // example.cpp is this file
    std::ifstream in("./example.cpp", in.binary);
    
    // by-character operations

    int c = in.get(); // Read one character
    // static_cast<char>(c) == '#'

    std::println("c == {}", static_cast<char>(c));

    // Seeking, tellg() returns current position
    auto pos = in.tellg();
    // Seek to zero bytes from end (in.beg for begining, in.cur for current position)
    in.seekg(0, in.end);

    // If we try to read past the last character, we get std::char_traits<char>::eof()
    c = in.get();
    // c == std::char_traits<char>::eof()

    std::println("c == {}, std::char_traits<char>::eof() == {}", c, std::char_traits<char>::eof());

    // Clear the eof flag and seek back 
    // to one after the first character
    in.clear();
    // Seek to the provided postion
    in.seekg(pos);
    
    c = in.peek(); // Peek at the next character (do not advance the position)
    // c == 'f'
    // same eof semantics as get()

    std::println("c == {}", static_cast<char>(c));

    in.unget(); // "unread" the last read character
    // implemented by adjusting buffer positions, and if that
    // isn't possible, the stream specific unget will be invoked,
    // which might not be supported

    c = in.get();
    // c == '#'

    std::println("c == {}", static_cast<char>(c));

    in.putback('!'); // "unread" with overwite
    // same semantics as unget(), in this case we can do it, 
    // since the buffer is mutable (the file isn't)

    std::vector<char> buff(64);

    // Read until the delimiter, at most the specified number of characters
    in.getline(buff.data(), 63, '\n');
    // buff.data() == "!include <fstream>"

    std::println("buff.data() == {}", buff.data());

    // Read until the delimiter at most the specified number of characters, 
    // without storing the read characters
    in.ignore(63, '\n');
    // skips over #include <print>

    buff = std::vector<char>(64);

    in.read(buff.data(), 17); // Read at most specified number of characters
    // buff.data() == "#include <vector>"

    std::println("buff.data() == {}", buff.data());

    buff = std::vector<char>(64);

    in.readsome(buff.data(), 1); // Same as read,
    // but will only read already available data in the buffer
    // buff[0] == '\n' (if in buffer)

    std::println("static_cast<int>(buff[0]) == {}", static_cast<int>(buff[0]));

    std::stringstream both;
   
    const char* str = "Is this a good day";
    both.write(str, strlen(str)); // Write the specified number of characters
    // from the pointed memory to the stream

    both.put('\n'); // Write a single character
    
    // Seek to absolute position
    both.seekp(0);
    const char* fix = "This is";
    both.write(fix, strlen(fix));

    // Seek the read position
    both.seekg(0, both.beg);

    buff = std::vector<char>(64);
    both.getline(buff.data(), 63, '\n');
    // buff.data() == "This is a good day"

    std::println("buff.data() == {}", buff.data());
}
```
Besides types, templates can also be parametrized using non-type arguments from a limited set of structural types.

In C++11, the selection was limited to integral types, enumerations and pointers/references to global objects. C++17 allowed auto.

C++20 added floating-point types and literal class types (that only have public members and are recursively structural).
https://compiler-explorer.com/z/941xxcPG5

```c++

// C++11
// integral, pointer, enumeration, or lvalue reference
template <typename T, std::size_t size>
struct MyArray {
    T data_[size];
    T* begin() { return data_; }
    T* end() { return data_ + size; }
};

struct Info { int a; double b; };
static constexpr Info global1{42, 3.14};
static constexpr Info global2{7, 1.61};

template <const Info &ref>
struct Printer {
    void print() {
        std::println("ref.a == {}, ref.b == {}", ref.a, ref.b);
    }
};

// C++17
// placeholder
template <auto value>
struct Generic {
    void print_info() {
        if constexpr (std::is_same_v<decltype(value), int>) {
            std::println("int with value : {}", value);
        } else if constexpr (std::is_same_v<decltype(value), const int*>) {
            std::println("pointer to int with value : {}", *value);
        }
    }
};

static constexpr int global3 = 42;

// C++20
// literal types, CTAD
template <auto fun, std::array arr>
struct Caller {
    auto call(auto&&... extra_args) {
        return fun(arr, std::forward<decltype(extra_args)>(extra_args)...);
    }
};

int main() {
    MyArray<int,3> r1{{1,2,3}};
    // decltype(r1.data_) == int[3]
    // r1.data_ == {1,2,3}

    static_assert(std::is_same_v<decltype(r1.data_), int[3]>);

    for (auto v : r1)
        std::print("{} ", v);
    std::println("");

    Printer<global1> r2;
    r2.print();
    // prints "ref.a == 42, ref.b == 3.14"

    Printer<global2> r3;
    r3.print();
    // prints "ref.a == 7, ref.b == 1.61"

    Generic<42> r4;
    r4.print_info();
    // prints "int with value : 42"

    Generic<&global3> r5;
    r5.print_info(); 
    // prints "pointer to int with value : 42"

    constexpr auto fn = [](const std::array<int,3> &arr, int x) {
        return arr[0] + arr[1] + arr[2] + x;
    };
    Caller<fn, std::array{1,2,3}> r6;
    int sum = r6.call(42);
    // sum == 48

    std::println("sum == {}", sum);
}
```
The std::thread is the C++ handle for a thread of execution. Multiple threads can be potentially executed concurrently.

A std::thread with an associated state must be joined, detached, or moved-from before it can be destroyed.

Joining a thread will block until the thread finishes running. Detaching a thread will dissociate it from the handle.

In C++20, prefer std::jthread.
https://compiler-explorer.com/z/WTc3oYGoE

```c++

std::atomic<int> total{};

int main() {
    // start a thread
    auto t1 = std::thread([]{ ++total; });
    // block until the thread finishes running
    t1.join();
    // total == 1

    std::println("total == {}", total.load());

    auto fn = [](int arg1, double arg2) {
        total += arg1 + arg2;
    };
    // start fn in a new thread with the arguments (42, 3.14)
    auto t2 = std::thread(fn, 42, 3.14);
    // block until the thread finishes running
    t2.join();
    // total == 46

    std::println("total == {}", total.load());

    size_t t_count = 4;
    std::latch work_done(t_count);
    auto work_fn = [&work_done]{
        // do work
        ++total;
        // mark that we are done
        work_done.count_down();
    };

    // Start four detached threads
    for (size_t i = 0; i < t_count; ++i)
        std::thread(work_fn).detach();

    work_done.wait(); // Wait until all threads finish
    // total == 50

    std::println("total == {}", total.load());
}
```
Pack indexing is a small but impactful feature that is very likely to land in C++26.

This feature allows us to directly access specific elements in a parameter pack, which was previously only possible through awkward boolean expression crafting or recursive templates.
https://compiler-explorer.com/z/nGMfh761n

```c++

// Basic use
template <typename... Ts>
using first_t = Ts...[0];

template <typename... Ts>
using last_t = Ts...[sizeof...(Ts)-1];

// first_t<int,double,float> == int
// last_t<int,double,float> == float

// A complex example for truncating a std::variant to unique list of types
template <typename... Ts>
struct remix {
    // Base template relying on pack indexing
    template <std::size_t... Is>
    struct variant { using type = std::variant<Ts...[Is]...>; };

// remix<int,double,float>::variant<0,1,2>::type == std::variant<int,double,float>
// remix<int,double,float>::variant<2,0,1>::type == std::variant<float,int,double>
// remix<int,double,float>::variant<2,0>::type == std::variant<float,int>

private:
    template <auto Arr, std::size_t... Is>
    constexpr static auto array_impl(std::index_sequence<Is...>) -> variant<Arr[Is]...>;
public:
    // Helper that operates using an array of indices instead of a pack of indices
    template <auto Arr>
    using variant_t = decltype(array_impl<Arr>(std::make_index_sequence<Arr.size()>{}))::type;

// remix<int,double,float>::variant_t<std::array{0,1,2}> == std::variant<int,double,float>
// remix<int,double,float>::variant_t<std::array{2,0,1}> == std::variant<float,int,double>
// remix<int,double,float>::variant_t<std::array{2,0}> == std::variant<float,int>
};

// Main logic:
// produce an array of indices that correspond to the unique types
template <typename... Ts>
consteval auto make_unique_idxs() {
    constexpr auto unique_idxs = []<typename... Types>{
        // Array of std::type_info
        std::array<std::reference_wrapper<const std::type_info>, sizeof...(Types)> types{{typeid(Types)...}};

        // Array which will contain the indices of unique types
        std::array<size_t, types.size()> idxs;
        std::iota(idxs.begin(), idxs.end(), 0);

        // std::type_info ordering isn't constexpr, but operator== is
        // so we can at least do a O(n*n) unique filter
        auto last = std::ranges::remove_if(idxs, [&](size_t idx) {
            return std::ranges::contains(types | std::views::take(idx),
                types[idx].get(),
                [](auto e) -> const std::type_info& { return e.get(); });
        });

        // last.begin()-idxs.begin() isn't a constant expression in this context,
        // so we can't directly return the correctly sized array
        // Therefore we return the full sized array and the calculated size.
        return std::pair{idxs, last.begin()-idxs.begin()};
    };
    // Helper that transforms a pair of array and size to the truncated array
    constexpr auto truncate_array = []<auto pair>{
        std::array<size_t, pair.second> result;
        std::ranges::copy_n(pair.first.begin(), pair.second, result.begin());
        return result;
    };
    return truncate_array.template operator()<unique_idxs.template operator()<Ts...>()>();
}

// Convenience wrappers
template <typename T> struct unique;
template <typename... Ts> struct unique<std::variant<Ts...>> {
    using type = remix<Ts...>::template variant_t<make_unique_idxs<Ts...>()>;
};
template <typename T>
using unique_t = unique<T>::type;

// unique_t<std::variant<int>> == std::variant<int>
// unique_t<std::variant<int,int,double>> == std::variant<int,double>
// unique_t<std::variant<int,double,int>> == std::variant<int,double>

int main() {
    using x1 = first_t<int,double,float>;
    static_assert(std::is_same_v<x1,int>);

    using x2 = last_t<int,double,float>;
    static_assert(std::is_same_v<x2,float>);


    using a1 = remix<int,double,float>::variant<0,1,2>::type;
    static_assert(std::is_same_v<a1,std::variant<int,double,float>>);

    using a2 = remix<int,double,float>::variant<2,0,1>::type;
    static_assert(std::is_same_v<a2,std::variant<float,int,double>>);

    using a3 = remix<int,double,float>::variant<2,0>::type;
    static_assert(std::is_same_v<a3,std::variant<float,int>>);


    using b1 = remix<int,double,float>::variant_t<std::array{0,1,2}>;
    static_assert(std::is_same_v<b1,std::variant<int,double,float>>);

    using b2 = remix<int,double,float>::variant_t<std::array{2,0,1}>;
    static_assert(std::is_same_v<b2,std::variant<float,int,double>>);

    using b3 = remix<int,double,float>::variant_t<std::array{2,0}>;
    static_assert(std::is_same_v<b3,std::variant<float,int>>);


    using c1 = unique_t<std::variant<int>>;
    static_assert(std::is_same_v<c1, std::variant<int>>);

    using c2 = unique_t<std::variant<int,int,double>>;
    static_assert(std::is_same_v<c2, std::variant<int,double>>);

    using c3 = unique_t<std::variant<int,double,int>>;
    static_assert(std::is_same_v<c3, std::variant<int,double>>);
}
```
An awkward problem during API design is distinguishing between inserting a single element and inserting a range of elements when the argument qualifies for both cases.

This can be solved in various ways (for example, using tagging); however, since C++23, we now have an official solution to this problem in the form of std::ranges::elements_of (used by std::generator).
https://compiler-explorer.com/z/zMvsqsfG4

```c++

struct MyStorage {
    using variant_t = std::variant<std::string,std::vector<std::string>>;

    // Push back a single element
    void push_back(std::convertible_to<variant_t> auto&& el) {
        std::println("Single element");
        data.push_back(std::forward<decltype(el)>(el));
    }

    // Push back a range of elements
    template <std::ranges::range R, typename Alloc>
    void push_back(std::ranges::elements_of<R,Alloc> wrap) {
        if constexpr (std::is_rvalue_reference_v<R>) {
            std::println("Multiple r-value elements");
            // Move iterator to maintain move-semantics
            data.insert(data.end(), 
                std::move_iterator{wrap.range.begin()},
                std::move_iterator{wrap.range.end()});
        } else {
            std::println("Multiple l-value elements");
            data.insert(data.end(), wrap.range.begin(), wrap.range.end());
        }
    }
    std::vector<variant_t> data;
};


int main() {
    MyStorage data;
    data.push_back("Hello World!"); // Single element
    data.push_back(std::vector<std::string>{"a","b"}); // Single element

    // Range of elements, r-value path
    data.push_back(std::ranges::elements_of(std::vector<std::string>{"a","b"}));

    std::vector<std::string> x{"a","b"};

    // Range of elements, l-value path
    data.push_back(std::ranges::elements_of(x));
    // Range of elements, r-value path
    data.push_back(std::ranges::elements_of(std::move(x)));
}
```
C++20 introduced a significant change to lambdas without capture. The autogenerated unique type is now default-constructible and copy (and move) assignable.

While the previous behaviour made sense for lambdas with captures and conceptually (each lambda is inherently an instance of its autogenerated type), it also made lambdas awkward to use as arguments to generic code.
https://compiler-explorer.com/z/TY4oqYccc

```c++

int main() {
    // Using a function object
    struct Cmp {
        bool operator()(int l, int r) const {
            return l < r;
        }
    } cmp;

    std::map<int,int,Cmp> a; // OK
    std::map<int,int,decltype(cmp)> b; // OK
    a = b; // OK

    // Same logic using a lambda
    auto lcmp = [](int l, int r) { return l < r; };

    std::map<int,int,decltype(lcmp)> c(lcmp); // OK
    // pre-C++20 we have to copy-construct the comparator

    std::map<int,int,decltype(lcmp)> d; // Wouldn't compile pre-C++20
    // pre-C++20 delctype(lcmp) isn't default constructible

    c = d; // Wouldn't compile pre-C++20
    // pre-C++20 decltype(lcmp) isn't copy-assignable
}
```
