
package CheckManager.Utils;

import java.util.Random;

public class Rand
{
    private final Random rand;

    public Rand()
    {
        rand = new Random();
    }

    public synchronized boolean	nextBoolean()
    {
            return rand.nextBoolean();
    }

    public synchronized void nextBytes(byte[] bytes)
    {
            rand.nextBytes(bytes);
    }

    public synchronized double nextDouble()
    {
            return rand.nextDouble();
    }

    public synchronized float nextFloat()
    {
           return rand.nextFloat();
    }

    public synchronized double nextGaussian()
    {
            return rand.nextGaussian();
    }

    public synchronized int nextInt()
    {
           return rand.nextInt();
    }

    public synchronized int nextInt(int n)
    {
            return rand.nextInt(n);
    }

    public synchronized long nextLong()
    {
            return rand.nextLong();
    }

    public synchronized int getRandom(int min, int max)
    {
        int res = rand.nextInt(max);
        res =   res < min ? res + min : res;
        return res;
    }

}
