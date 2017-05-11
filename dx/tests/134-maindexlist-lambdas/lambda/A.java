package lambda;

public class A {

  public static void main(String[] args) {
    new A().run(new B()::doIt);
    new A().run(B::doItStatic);
  }

  public void run(Runnable todo) {
    todo.run();
  }

}
