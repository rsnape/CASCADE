package uk.ac.cranfield.cascade.market;

public class Sync {
	int c = 0;
	
	public Sync(int c)
	{
		this.c = c;
	}
	
	public synchronized void inc()
	{
		c++;
	}
	
	public synchronized void dec()
	{
		if(c > 0)
		   c--;
		if(c==0) this.notifyAll();
	}
	
	public synchronized boolean isZero()
	{
		if (c==0) return true;
		else return false;
	}
	
	public synchronized void waitZero()
	{
		while(!isZero())
		{
			try {
				this.wait();
			} catch (InterruptedException e) {
			}
		}
	}
	
	

}
