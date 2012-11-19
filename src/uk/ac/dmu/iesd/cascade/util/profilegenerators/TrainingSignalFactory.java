/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util.profilegenerators;

import java.util.Arrays;

/**
 * @author jsnape
 *
 */
public class TrainingSignalFactory {
	private int defaultLength = 48;
	private double[] PBsigFromMatlab = new double[]{0,0.166666667,0.25,0.333333333,0.416666667,0.5,0.583333333,0.666666667,0.75,0.833333333,0.916666667,1,1,0.916666667,0.833333333,0.75,0.666666667,0.583333333,0.5,0.416666667,0.333333333,0.25,0.166666667,0,0,-0.166666667,-0.25,-0.333333333,-0.416666667,-0.5,-0.583333333,-0.666666667,-0.75,-0.833333333,-0.916666667,-1,-1,-0.916666667,-0.833333333,-0.75,-0.666666667,-0.583333333,-0.5,-0.416666667,-0.333333333,-0.25,-0.166666667,0};

	private int signalLength;
	public static enum SIGNAL_TYPE {IMPULSE, TRIANGLE, SQUARE, SINE, PBORIGINAL, COSINE};
		
	public void setSignalLength(int n)
	{
		this.signalLength = n;
	}
	
	public int getSignalLength()
	{
		return this.signalLength;
	}
	
	public double[] generateSignal(SIGNAL_TYPE sigType)
	{
		return generateSignal(sigType, this.defaultLength);
	}
	
	public double[] generateSignal(SIGNAL_TYPE sigType, int n)
	{
		double [] ret = new double[n];
		switch (sigType){
			case IMPULSE:
				ret =  genImpulseSignal(n);
				break;
			case TRIANGLE:
				ret =  genTriangularSignal(n);
				break;
			case SQUARE:
				ret =  genSquareSignal(n);
				break;
			case SINE:
				ret =  genSineSignal(n);
				break;
			case COSINE:
				ret =  genCosineSignal(n);
				break;
			case PBORIGINAL:
				ret = Arrays.copyOf(this.PBsigFromMatlab, this.PBsigFromMatlab.length);
				break;
			default:
				System.err.println("Unrecognised signal type requested, returning null");
				ret =  null;
				break;
		}
		
		return ret;
	}
	
	/**
	 * @param n
	 * @return
	 */
	private double[] genCosineSignal(int n)
	{
		double[] ret = new double[n];
		double[] t = genSineSignal(n);
		int indexFor1 = n/4;
		System.arraycopy(t, indexFor1, ret, 0, t.length - indexFor1);
		System.arraycopy(t, 0, ret, t.length - indexFor1, indexFor1);

		return ret;
	}

	/**
	 * @param n
	 * @return
	 */
	private double[] genSquareSignal(int n) {
		double[] ret = new double[n];
		Arrays.fill(ret, 1);
		Arrays.fill(ret, n/2, n, -1);
		return ret;
	}

	/**
	 * Note that slightly awkward way of doing this is to get a true zero at the mid point
	 * rather than a "nearly zero" due to double precision.
	 * 
	 * @param n
	 * @return
	 */
	private double[] genSineSignal(int n) {
		int halfWave = n / 2;
		double deltaT = 2*Math.PI / n;
		double[] ret = new double[n];
		for (int t = 0; t < halfWave; t++)
		{
			double val = Math.sin(t*deltaT);
			ret[t] = val;
			ret[t+halfWave] = -val;
		}
		//ret[halfWave-1] = 0; 
		return ret;
	}

	/**
	 * @param n
	 * @return
	 */
	private double[] genTriangularSignal(int n) {
		int period = n;
		int halfWave = n / 4;
		int deadBand = 0;
		if (n%4 != 0)
		{
			deadBand = n - (4*halfWave);
		}
		
		double delta = 1d / halfWave;
		double[] ret = new double[n];
		for (int t = 0; t < n; t++)
		{
			if (t < halfWave)
			{
				ret[t] = t*delta;
			}
			else if (t<2*halfWave)
			{
				ret[t] = 1 - (t-halfWave)*delta;
			}
			else if (t < (2*halfWave+deadBand))
			{
				ret[t] = 0;
			}
			else if (t < 3*halfWave+deadBand)
			{
				ret[t] = 0 - (t - (2*halfWave+deadBand))*delta;
			}
			else
			{
				ret[t] = (t - (3*halfWave+deadBand))*delta - 1;
			}
		}
		return ret; 
	}

	/**
	 * @param n
	 * @return
	 */
	private double[] genImpulseSignal(int n) {
		double[] ret = new double[n];
		Arrays.fill(ret, -1d/(n-1));
		ret[0] = 1;
		return ret;
	}

	public TrainingSignalFactory()
	{
		super();
		this.setSignalLength(this.defaultLength);
	}
	
	public TrainingSignalFactory(int n)
	{
		super();
		this.setSignalLength(n);
	}
	
}
