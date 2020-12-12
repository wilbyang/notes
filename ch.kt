fun start(): String = "OK"


## Default and named arguments 

fun joinOptions(options: Collection<String>) =
        options.joinToString(prefix = "[", postfix = "]")

fun foo(name: String, number: Int = 42, toUpperCase: Boolean = false) =
        (if (toUpperCase) name.toUpperCase() else name) + number


val month = "(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)"

fun getPattern(): String = """\d{2} $month \d{4}"""


val month = "(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)"

fun getPattern(): String = """\d{2} $month \d{4}"""


## nullable

fun sendMessageToClient(
        client: Client?, message: String?, mailer: Mailer
) {
    val email = client?.personalInfo?.email
    if (email != null && message != null) {
        mailer.sendMessage(email, message)
    }
}



class Client(val personalInfo: PersonalInfo?)
class PersonalInfo(val email: String?)
interface Mailer {
    fun sendMessage(email: String, message: String)
}



## Nothing type

import java.lang.IllegalArgumentException

fun failWithWrongAge(age: Int?): Nothing {
    throw IllegalArgumentException("Wrong age: $age")
}

fun checkAge(age: Int?) {
    if (age == null || age !in 0..150) failWithWrongAge(age)
    println("Congrats! Next year you'll be ${age + 1}.")
}

fun main() {
    checkAge(10)
}

## lamda
fun containsEven(collection: Collection<Int>): Boolean =
        collection.any { it % 2 == 0 }

## data class, equals/hashCode, toString and some others are generated

data class Person(val name: String, val age: Int)

fun getPeople(): List<Person> {
    return listOf(Person("Alice", 29), Person("Bob", 31))
}

fun comparePeople(): Boolean {
    val p1 = Person("Alice", 29)
    val p2 = Person("Alice", 29)
    return p1 == p2  // should be true
}


##smart cast

fun eval(expr: Expr): Int =
        when (expr) {
            is Num -> expr.value
            is Sum -> eval(expr.left) + eval(expr.right)
            else -> throw IllegalArgumentException("Unknown expression")
        }

interface Expr
class Num(val value: Int) : Expr
class Sum(val left: Expr, val right: Expr) : Expr


## sealed class

fun eval(expr: Expr): Int =
        when (expr) {
            is Num -> expr.value
            is Sum -> eval(expr.left) + eval(expr.right)
        }

sealed class Expr
class Num(val value: Int) : Expr()
class Sum(val left: Expr, val right: Expr) : Expr()

## rename on import

 import kotlin.random.Random as KRandom
 import java.util.Random as JRandom

fun useDifferentRandomClasses(): String {
    return "Kotlin random: " +
             KRandom.nextInt(2) +
            " Java random:" +
             JRandom().nextInt(2) +
            "."
}

## extension method

fun Int.r(): RationalNumber = RationalNumber(this, 1)

fun Pair<Int, Int>.r(): RationalNumber = RationalNumber(first, second)

data class RationalNumber(val numerator: Int, val denominator: Int)

## operator overloading 

data class MyDate(val year: Int, val month: Int, val dayOfMonth: Int) : Comparable<MyDate> {
    override fun compareTo(other: MyDate) = when {
        year != other.year -> year - other.year
        month != other.month -> month - other.month
        else -> dayOfMonth - other.dayOfMonth
    }
}

fun test(date1: MyDate, date2: MyDate) {
    // this code should compile:
    println(date1 < date2)
}

## iterator
data class MyDate(val year: Int, val month: Int, val dayOfMonth: Int) : Comparable<MyDate> {
    override fun compareTo(other: MyDate): Int {
        if (year != other.year) return year - other.year
        if (month != other.month) return month - other.month
        return dayOfMonth - other.dayOfMonth
    }
}

operator fun MyDate.rangeTo(other: MyDate) = DateRange(this, other)

import java.util.Calendar

/*
 * Returns the following date after the given one.
 * For example, for Dec 31, 2019 the date Jan 1, 2020 is returned.
 */
fun MyDate.followingDate(): MyDate {
    val c = Calendar.getInstance()
    c.set(year, month, dayOfMonth)
    val millisecondsInADay = 24 * 60 * 60 * 1000L
    val timeInMillis = c.timeInMillis + millisecondsInADay
    val result = Calendar.getInstance()
    result.timeInMillis = timeInMillis
    return MyDate(result.get(Calendar.YEAR), result.get(Calendar.MONTH), result.get(Calendar.DATE))
}

class DateRange(val start: MyDate, val end: MyDate)

fun iterateOverDateRange(firstDate: MyDate, secondDate: MyDate, handler: (MyDate) -> Unit) {
    for (date in firstDate..secondDate) {
        handler(date)
    }
}



## collections


data class Shop(val name: String, val customers: List<Customer>)

data class Customer(val name: String, val city: City, val orders: List<Order>) {
    override fun toString() = "$name from ${city.name}"
}

data class Order(val products: List<Product>, val isDelivered: Boolean)

data class Product(val name: String, val price: Double) {
    override fun toString() = "'$name' for $price"
}

data class City(val name: String) {
    override fun toString() = name
}

fun Shop.getSetOfCustomers(): Set<Customer> =
        customers.toSet()

fun Shop.getCustomersSortedByOrders(): List<Customer> =
        customers.sortedByDescending { it.orders.size }


fun Shop.getCustomerCities(): Set<City> =
        customers.map { it.city }.toSet()


fun Shop.getCustomersFrom(city: City): List<Customer> =
        customers.filter { it.city == city }


fun Shop.checkAllCustomersAreFrom(city: City): Boolean =
        customers.all { it.city == city }


fun Shop.hasCustomerFrom(city: City): Boolean =
        customers.any { it.city == city }


fun Shop.countCustomersFrom(city: City): Int =
        customers.count { it.city == city }


fun Shop.findCustomerFrom(city: City): Customer? =
        customers.find { it.city == city }

fun Shop.getCustomerWithMaxOrders(): Customer? =
        customers.maxBy { it.orders.size }


fun getMostExpensiveProductBy(customer: Customer): Product? =
        customer.orders
                .flatMap(Order::products)
                .maxBy(Product::price)

fun moneySpentBy(customer: Customer): Double =
        customer.orders.flatMap { it.products }.sumByDouble { it.price }

fun Shop.groupCustomersByCity(): Map<City, List<Customer>> =
        customers.groupBy { it.city }

fun Shop.nameToCustomerMap(): Map<String, Customer> =
        customers.associateBy(Customer::name)


fun Shop.customerToCityMap(): Map<Customer, City> =
        customers.associateWith(Customer::city)


fun Shop.customerNameToCityMap(): Map<String, City> =
        customers.associate { it.name to it.city }


fun Shop.getCustomersWithMoreUndeliveredOrders(): Set<Customer> = customers.filter {
    val (delivered, undelivered) = it.orders.partition { it.isDelivered }
    undelivered.size > delivered.size
}.toSet()


fun Customer.getOrderedProducts(): List<Product> =
        orders.flatMap(Order::products)


fun Shop.getOrderedProducts(): Set<Product> =
        customers.flatMap(Customer::getOrderedProducts).toSet()


fun Shop.getProductsOrderedByAll(): Set<Product> {
    val allProducts = customers.flatMap { it.getOrderedProducts() }.toSet()
    return customers.fold(allProducts, { orderedByAll, customer ->
        orderedByAll.intersect(customer.getOrderedProducts())
    })
}

fun Customer.getOrderedProducts(): List<Product> =
        orders.flatMap(Order::products)


fun findMostExpensiveProductBy(customer: Customer): Product? {
    return customer
            .orders
            .filter(Order::isDelivered)
            .flatMap(Order::products)
            .maxBy(Product::price)
}


fun Shop.getNumberOfTimesProductWasOrdered(product: Product): Int {
    return customers
            .flatMap(Customer::getOrderedProducts)
            .count { it == product }
}

fun Customer.getOrderedProducts(): List<Product> =
        orders.flatMap(Order::products)


fun findMostExpensiveProductBy(customer: Customer): Product? {
    return customer
            .orders
            .asSequence()
            .filter(Order::isDelivered)
            .flatMap { it.products.asSequence() }
            .maxBy(Product::price)
}


fun Shop.getNumberOfTimesProductWasOrdered(product: Product): Int {
    return customers
            .asSequence()
            .flatMap(Customer::getOrderedProducts)
            .count { it == product }
}

fun Customer.getOrderedProducts(): Sequence<Product> =
        orders.flatMap(Order::products).asSequence()


## properties

class PropertyExample() {
    var counter = 0
    var propertyWithCounter: Int? = null
        set(v) {
            
            field = v
            counter++
        }
}

## lazy
class LazyProperty(val initializer: () -> Int) {
    var value: Int? = null
    val lazy: Int
        get() {
            if (value == null) {
                value = initializer()
            }
            return value!!
        }
}

##
class LazyProperty(val initializer: () -> Int) {
    val lazyValue: Int by lazy(initializer)
}



##

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class D {
    var date: MyDate by EffectiveDate()
}

class EffectiveDate<R> : ReadWriteProperty<R, MyDate> {

    var timeInMillis: Long? = null

    override fun getValue(thisRef: R, property: KProperty<*>): MyDate {
        return timeInMillis!!.toDate()
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: MyDate) {
        timeInMillis = value.toMillis()
    }
}





