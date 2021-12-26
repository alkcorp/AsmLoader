package alk.test;

public class HelloWorld {

	private static HelloWorld sHelloWorld;
	
	public static void main(String[] args) {		
		sHelloWorld = new HelloWorld();
		sHelloWorld.hello();
	}

	public void hello() {
		System.out.println("Hello World");
	}	
	
}
