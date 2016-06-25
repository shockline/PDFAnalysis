package util;

public class Util {
	public static float getMin(float f1, float f2, float f3) {
		float toReturn = f1;
		if (toReturn > f2)
			toReturn = f2;
		if (toReturn > f3)
			toReturn = f3;
		return toReturn;
	}

	public static float getMax(float f1, float f2, float f3) {
		float toReturn = f1;
		if (toReturn < f2)
			toReturn = f2;
		if (toReturn < f3)
			toReturn = f3;
		return toReturn;
	}
}
