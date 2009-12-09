import java.lang.annotation.Annotation;
import java.util.*;


public aspect Main {

	dependent before next(Iterator i): call(* java.util.Iterator+.next()) && target(i) {}
	dependent before hasNext(Iterator i): call(* java.util.Iterator+.hasNext()) && target(i) {}
	
	dependency {
		hasNext, next;
		initial s0: next -> s1;
				s1: next -> s2;
		final s2;		
	}
	
	public static void main(String args[]) {
		Collection c = new HashSet();
		Iterator i = c.iterator();
		i.next();
		foo(i);
	}
	
	
	@Precon({"i.hasNext"})
	static void foo(Iterator i) {
		i.next();
	}

}
