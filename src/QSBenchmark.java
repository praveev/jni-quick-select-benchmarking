
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Random;
import sun.misc.Unsafe;

/**
 * Created by pv1 on 7/29/16.
 */
public class QSBenchmark {

    private native double totalCppTime0();
    private native double selectTime0();
    private native double writeTime0();
    private native double quickSelect0(Class cls, long address, int lo, int hi, int pivot);
    private static final Random random = new Random(); // pseudo-random number generator

    static {
        System.loadLibrary("foo");
    }

    public static void shuffle(LongBuffer a) {
        int N = a.limit();
        for (int i = 0; i < N; i++ ) {
            int r = i + random.nextInt(N - i); // between i and N-1
            long temp = a.get(i);
            a.put(i, a.get(r));
            a.put(r, temp);
        }
    }

    public static void shuffle(int len, long startAddress, Unsafe unsafe) {
        for (int i = 0; i < len; i++ ) {
            long r = i + random.nextInt(len - i); // between i and N-1
            long temp = unsafe.getLong(null, startAddress + (8 * i));
            unsafe.putLong(null, startAddress + (8 * i), unsafe.getLong(null, startAddress + (8 * r)));
            unsafe.putLong(null, startAddress + (8 * r), temp);
        }
    }


    public void checkQuickSelectUnsafeBased() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        System.out.println("************************");
        double duration = 0;
        double writeTime = 0;
        double selectTime = 0;
        double totalCppTime = 0;
        int len = 64;
        int nonZeros = 64;

        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        long startAddress = unsafe.allocateMemory(len * 8);

        for (int i = 0; i < nonZeros; i++ ) {
            unsafe.putLong(null, startAddress + (i * 8), i+1);
        }

        for (int pivot = 0; pivot < len; pivot++ ) {
            for (int i = 0; i < 100000; i++ ) {
                shuffle(len, startAddress, unsafe);
                long min = System.nanoTime();
                double retVal = quickSelect0(Long.class, startAddress, 0, len - 1, pivot);
                duration += (System.nanoTime() - min);
                writeTime += writeTime0();
                selectTime += selectTime0();
                totalCppTime += totalCppTime0();
                long idxVal = unsafe.getLong(null, startAddress + (8 * pivot));
                if (retVal != idxVal) {
                    System.out.println("BAD!!! " + retVal + " vs " + idxVal);
                }
            }
        }
        System.out.println("Unsafe k=64 - Total duration: " + duration + "ns. " +
                "Write Time: " + writeTime + "ns " + "Select Time: " + selectTime + "ns " +
                "Total C++ time: " + totalCppTime + "ns"
        );
    }


    public void checkQuickSelectDBBBased() {
        System.out.println("************************");
        double duration = 0;
        double writeTime = 0;
        double selectTime = 0;
        double totalCppTime = 0;
        int len = 64;
        int nonZeros = 64;
        ByteBuffer arr = ByteBuffer.allocateDirect(len * 8);
        arr.order( java.nio.ByteOrder.LITTLE_ENDIAN );
        for (int i = 0; i < nonZeros; i++ ) {
            arr.putLong(i, i+1);
        }
        LongBuffer lBuf = arr.asLongBuffer();
        for (int pivot = 0; pivot < 64; pivot++ ) {
            for (int i = 0; i < 100000; i++ ) {
                shuffle(lBuf);
                long min = System.nanoTime();
                double retVal = quickSelect0(Long.class, ((sun.nio.ch.DirectBuffer)lBuf).address(), 0, len - 1, pivot);
                duration += (System.nanoTime() - min);
                writeTime += writeTime0();
                selectTime += selectTime0();
                totalCppTime += totalCppTime0();
                long idxVal = lBuf.get(pivot);
                if (retVal != idxVal) {
                    System.out.println("BAD!!! " + retVal + " vs " + idxVal);
                }
            }
        }
        System.out.println("DBB k=64 - Total duration: " + duration + "ns. " +
                "Write Time: " + writeTime + "ns " + "Select Time: " + selectTime + "ns " +
                "Total C++ time: " + totalCppTime + "ns"
        );
    }


    private void tryUnsafe() throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
        unsafeConstructor.setAccessible(true);
        Unsafe unsafe = unsafeConstructor.newInstance();

        long startAddress = unsafe.allocateMemory(5 * 8);
        unsafe.putLong(null, startAddress, 12);
        unsafe.putLong(null, startAddress + 8, 4);
        unsafe.putLong(null, startAddress + 16, 9);
        unsafe.putLong(null, startAddress + 24, 7);
        unsafe.putLong(null, startAddress + 32, 8);

        long min = System.nanoTime();
        double retVal = this.quickSelect0(Long.class, startAddress, 0, 4, 2);
        System.out.println("***************");
        System.out.println("Total duration: " + (System.nanoTime() - min) + "ns");
        System.out.println("Unsafe write time -> " + writeTime0() + "ns");
        System.out.println("Unsafe select time -> " + selectTime0() + "ns");
        System.out.println("Unsafe total c++ time -> " + totalCppTime0() + "ns");
    }

    private void tryDBBQuickSelect() {
        ByteBuffer buf = ByteBuffer.allocateDirect(5 * 8);
        buf.order( java.nio.ByteOrder.LITTLE_ENDIAN );

        buf.putLong(12);
        buf.putLong(4);
        buf.putLong(9);
        buf.putLong(7);
        buf.putLong(8);

        buf.rewind();

        LongBuffer lBuf = buf.asLongBuffer();

        long min = System.nanoTime();
        double retVal = this.quickSelect0(Long.class, ((sun.nio.ch.DirectBuffer)lBuf).address(), 0, lBuf.remaining() - 1, 2);
        System.out.println("***************");
        System.out.println("Total duration: " + (System.nanoTime() - min) + "ns");
        System.out.println("DBB write time -> " + writeTime0() + "ns");
        System.out.println("DBB select time -> " + selectTime0() + "ns");
        System.out.println("DBB total c++ time -> " + totalCppTime0() + "ns");
    }

    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        // dummy calls
        QSBenchmark f = new QSBenchmark();
        f.tryDBBQuickSelect();
        f.tryUnsafe();

        // real calls
        QSBenchmark c = new QSBenchmark();
        c.tryUnsafe();
        c.tryDBBQuickSelect();
        c.tryUnsafe();
        c.checkQuickSelectDBBBased();
        c.checkQuickSelectUnsafeBased();
    }
}
