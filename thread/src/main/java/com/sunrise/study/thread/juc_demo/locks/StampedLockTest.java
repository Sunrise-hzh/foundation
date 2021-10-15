package com.sunrise.study.thread.juc_demo.locks;

import java.util.concurrent.locks.StampedLock;

/**
 * @author huangzihua
 * @date 2021-09-17
 */
public class StampedLockTest {

    private double x, y;

    // 锁实例
    private final StampedLock sl = new StampedLock();

    public static void main(String[] args) throws InterruptedException {
        StampedLockTest demo = new StampedLockTest();
        StampedLockThread t1 = new StampedLockThread(demo, "thread-01");
        StampedLockThread t2 = new StampedLockThread(demo, "thread-02");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("end");
    }

    static class StampedLockThread extends Thread {
        private StampedLockTest test;
        public StampedLockThread(StampedLockTest test, String name) {
            super(name);
            this.test = test;
        }

        @Override
        public void run() {
            test.move(10, 20);
        }
    }

    // 排它锁 -- 写锁（writeLock）
    void move(double deltaX, double deltaY) {
        long stamp = sl.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    // 乐观读锁--tryOptimisticRead
    double distanceFromOrigin() {
        // (1)尝试获取乐观读锁
        long stamp = sl.tryOptimisticRead();
        // (2)将全部变量复制到方法体栈内
        double currentX = x;
        double currentY = y;
        // (3)检查在（1）处获取了读锁戳记后，锁有没被其他写线程排它性抢占
        if (!sl.validate(stamp)) {
            // (4)如果被抢占则获取一个共享读锁（悲观获取）
            stamp = sl.readLock();
            try {
                // (5)将全部变量复制到方法体栈内
                currentX = x;
                currentY = y;
            } finally {
                // (6)释放共享读锁
                sl.unlockRead(stamp);
            }
        }
        // (7)返回计算结果
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    // 使用悲观锁获取读锁，并尝试转换为写锁
    void moveIfAtOrigin(double newX, double newY) {
        // (1)这里可以使用乐观读锁替换
        long stamp = sl.readLock();
        try {
            // (2)如果当前点在原点则移动
            while (x == 0.0 && y == 0.0) {
                // (3)尝试将获取的读锁升级为写锁
                long ws = sl.tryConvertToWriteLock(stamp);
                // (4)升级成功，则更新戳记，并设置坐标值，然后退出循环
                if (ws != 0L) {
                    stamp = ws;
                    x = newX;
                    y = newY;
                    break;
                } else {
                    // (5)读锁升级写锁失败则释放读锁，显式获取独占写锁，然后循环重试
                    sl.unlockRead(stamp);
                    stamp = sl.writeLock();
                }
            }
        } finally {
            // (6)释放锁
            sl.unlock(stamp);
        }
    }
}
