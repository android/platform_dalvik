interface Consumer<T> {
    void accept(T t);
}

class Foo {
    int i;
    void bar(int j) {
        Consumer consumer = k -> System.out.println(((i + j) + (int)k));
        consumer.accept(1);
    }
}

class Main {
    public static void main(String[] args) {
        new Foo().bar(5);
    }
}
