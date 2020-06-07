package de.revout.pi.vplotter.converter;

import java.math.BigInteger;

public class BezierCurve {

	private int pointsCount;

	public BezierCurve(int paramPointsCount) {
		pointsCount = paramPointsCount;
	}

	public static BigInteger factorial(long number) {
		BigInteger result = BigInteger.valueOf(1);

		for (long factor = 2; factor <= number; factor++) {
			result = result.multiply(BigInteger.valueOf(factor));
		}

		return result;
	}

	private double ni(int n, int i) {
		double ni;
		double a1 = factorial(n).doubleValue();
		double a2 = factorial(i).doubleValue();
		double a3 = factorial(n - i).doubleValue();
		ni = a1 / (a2 * a3);
		return ni;
	}


	  // Calculate Bernstein basis
    private double bernstein(int n, int i, double t)
    {
        double basis;
        double ti; /* t^i */
        double tni; /* (1 - t)^i */

        if (t == 0.0 && i == 0) 
            ti = 1.0; 
        else 
            ti = Math.pow(t, i);

        if (n == i && t == 1.0) 
            tni = 1.0; 
        else 
            tni = Math.pow((1 - t), (n - i));

        basis = ni(n, i) * ti * tni; 
        return basis;
    }
    
    public double[] bezier2D(double[] b)
    {
    	double[] result = new double[pointsCount*2];
        int npts = (b.length) / 2;
        int icount, jcount;
        double step, t;


        icount = 0;
        t = 0;
        step = (double)1.0 / (pointsCount - 1);

        for (int i1 = 0; i1 < pointsCount; i1++)
        { 
            if ((1.0 - t) < 5e-6) 
                t = 1.0;

            jcount = 0;
            result[icount] = 0.0;
            result[icount + 1] = 0.0;
            for (int i = 0; i < npts; i++)
            {
                double basis = bernstein(npts - 1, i, t);
                result[icount] += basis * b[jcount];
                result[icount + 1] += basis * b[jcount + 1];
                jcount = jcount +2;
            }

            icount += 2;
            t += step;
        }
        return result;
    }    
    
    	
	
}
