package com.sunrise.study.thread.demo;



import java.lang.reflect.Field;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author huangzihua
 * @date 2021-08-09
 */
public class LockDemo {
    private static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
//        MyThread thread = new MyThread(lock, "t1");
//        MyThread thread2 = new MyThread(lock, "t2");
//        thread.start();
//        thread2.start();
//        thread.join();
//        thread2.join();

//        for (int i = 0; i < 10; i++) {
//            Thread thread = new MyThread(lock, "t" + i);
//            thread.start();
//        }
//        Thread.sleep(3000);

        Apple apple = new Apple(lock, 1);
        Thread eatThread = new EatThread(apple, "【吃货1号】");
        Thread eatThread2 = new EatThread(apple, "【吃货2号】");
        Thread eatThread3 = new EatThread(apple, "【吃货3号】");
        eatThread.start();
        eatThread2.start();
        eatThread3.start();
        Thread addThread = new AddThread(apple, "【生产商1号】");
        Thread addThread2 = new AddThread(apple, "【生产商2号】");
        addThread.start();
        addThread2.start();
        System.out.println("main ending");
    }

    public static void print(Condition addCondition, Condition eatCondition, String pre) throws NoSuchFieldException, IllegalAccessException {
        Class<ReentrantLock> lockClass = ReentrantLock.class;
        Field sync = lockClass.getDeclaredField("sync");
        sync.setAccessible(true);
        Object syncObject = sync.get(lock);
        Class<?> nonfairSyncClass = syncObject.getClass();
        Class<?> syncClass = nonfairSyncClass.getSuperclass();
        Class<?> aqsClass = syncClass.getSuperclass();
        Field[] aqsFields = aqsClass.getDeclaredFields();
        StringBuilder printStr = new StringBuilder();
        Object headNode = null;
        for (Field aqsField : aqsFields) {
            if ("state".equals(aqsField.getName())) {
                aqsField.setAccessible(true);
                Object value = aqsField.get(syncObject);
//                System.out.println(aqsField.getName() + " = " + value);
                printStr.append(aqsField.getName() + " = " + value + ", ");
            }
            if ("head".equals(aqsField.getName())) {
                aqsField.setAccessible(true);
                headNode = aqsField.get(syncObject);
            }
        }

        Class<?> aosClass = aqsClass.getSuperclass();
        Field exclusiveOwnerThreadField = aosClass.getDeclaredField("exclusiveOwnerThread");
        exclusiveOwnerThreadField.setAccessible(true);
        Thread exclusiveOwnerThread = (Thread) exclusiveOwnerThreadField.get(syncObject);
        printStr.append("exclusiveOwnerThread = " + (exclusiveOwnerThread == null ? null : exclusiveOwnerThread.getName()));

        if (headNode != null) {
            printStr.append(", node = ").append(printNode(headNode));
        }

        if (addCondition != null) {
            Class<? extends Condition> conditionClass = eatCondition.getClass();
            Field firstWaiterField = conditionClass.getDeclaredField("firstWaiter");
            firstWaiterField.setAccessible(true);
            Object eatFirstWaiterNode = firstWaiterField.get(eatCondition);
            Object addFirstWaiterNode = firstWaiterField.get(addCondition);
            if (eatFirstWaiterNode != null) {
                printStr.append(", eatCondition = ").append(printNode(eatFirstWaiterNode));
            }
            if (addFirstWaiterNode != null) {
                printStr.append(", addCondition = ").append(printNode(addFirstWaiterNode));
            }
        }

        System.out.println(pre + ": " + printStr);
    }

    private static String printNode(Object headNode) throws NoSuchFieldException, IllegalAccessException {
        Class<?> nodeClass = headNode.getClass();
        Field waitStatus = nodeClass.getDeclaredField("waitStatus");
        Field prev = nodeClass.getDeclaredField("prev");
        Field next = nodeClass.getDeclaredField("next");
        Field thread = nodeClass.getDeclaredField("thread");
        Field nextWaiter = nodeClass.getDeclaredField("nextWaiter");
        waitStatus.setAccessible(true);
        prev.setAccessible(true);
        next.setAccessible(true);
        thread.setAccessible(true);
        nextWaiter.setAccessible(true);

        Object curNode = headNode;
        StringBuilder nodeStr = new StringBuilder();

        while (curNode != null) {
            nodeStr.append("\t{ ");
            Object waitStatusObj = waitStatus.get(curNode);
            nodeStr.append("ws="+waitStatusObj);

            Object prevObj = prev.get(curNode);
            if (prevObj != null) {
                Object tObj = thread.get(prevObj);
                if (tObj != null) {
                    Thread v1 = (Thread) tObj;
                    nodeStr.append(", p="+v1.getName());
                } else
                    nodeStr.append(", p=empty");
            } else
                nodeStr.append(", p=null");

            Object threadObj = thread.get(curNode);
            if (threadObj != null) {
                Thread v1 = (Thread) threadObj;
                nodeStr.append(", t=" + v1.getName());
            } else
                nodeStr.append(", t=null");

            Object nextObj = next.get(curNode);
            if (nextObj != null) {
                Object tObj = thread.get(nextObj);
                if (tObj != null) {
                    Thread v1 = (Thread) tObj;
                    nodeStr.append(", n="+v1.getName());
                } else
                    nodeStr.append(", n=empty");
            } else
                nodeStr.append(", n=null");

            Object nextWaiterObj = nextWaiter.get(curNode);
            if (nextWaiterObj != null) {
                Object tObj = thread.get(nextWaiterObj);
                if (tObj != null) {
                    Thread v1 = (Thread) tObj;
                    nodeStr.append(", nw="+v1.getName());
                } else
                    nodeStr.append(", nw=empty");
            } else
                nodeStr.append(", nw=null");

            nodeStr.append(" }\n");
            curNode = nextObj;
            if (curNode == null) {
                curNode = nextWaiterObj;
            }
        }
        return "[ \n" + nodeStr.toString() + "]";
    }
}

class AddThread extends Thread {
    Apple apple;
    public AddThread(Apple apple, String name) {
        super(name);
        this.apple = apple;
    }

    @Override
    public void run() {
        apple.add(getName());
    }
}

class EatThread extends Thread {
    Apple apple;
    public EatThread(Apple apple, String name) {
        super(name);
        this.apple = apple;
    }

    @Override
    public void run() {
        apple.eat(getName());
    }
}

class Apple {
    ReentrantLock lock;
    Condition eatCondition;
    Condition addCondition;
    int count = 0;

    public Apple(ReentrantLock lock, int count) {
        this.lock = lock;
        eatCondition = lock.newCondition();
        addCondition = lock.newCondition();
        this.count = count;
    }

    public void eat(String name) {
        lock.lock();
        try {
            LockDemo.print(addCondition, eatCondition, name + " 得到锁资源");
            int num = 0;
            while (true) {
                LockDemo.print(addCondition, eatCondition,name + " 循环第" + num++ + "次");
                if (count <= 0) {
                    LockDemo.print(addCondition, eatCondition,name + " 释放锁资源，进入wait状态 ");
                    eatCondition.await();
                    LockDemo.print(addCondition, eatCondition,name + " wait结束，被唤醒，继续执行");
                }
                Thread.sleep(1000);
                count--;
                LockDemo.print(addCondition, eatCondition,name + " 吃了1个，还剩 " + count + "个");
                addCondition.signal();
                LockDemo.print(addCondition, eatCondition,name + " 唤醒生产者");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void add(String name) {
        lock.lock();
        try {
            LockDemo.print(addCondition, eatCondition,name + " 得到锁资源");
            int num = 0;
            while (true) {
                LockDemo.print(addCondition, eatCondition,name + " 循环第" + num++ + "次");
                if (count >= 3) {
                    LockDemo.print(addCondition, eatCondition,name + " 释放锁资源，进入wait状态 ");
                    addCondition.await();
                    LockDemo.print(addCondition, eatCondition,name + " wait结束，被唤醒，继续执行");
                }
                Thread.sleep(1000);
                count++;
                LockDemo.print(addCondition, eatCondition,name + " 生产了1个，还剩 " + count + "个");
                eatCondition.signal();
                LockDemo.print(addCondition, eatCondition,name + " 唤醒消费者");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

}


class MyThread extends Thread {
    ReentrantLock lock;

    MyThread(ReentrantLock lock, String name) {
        super(name);
        this.lock = lock;
    }

    @Override
    public void run() {

        try {
            System.out.println(getName() + ": start");
            LockDemo.print(null, null,getName() + " lock之前");
            lock.lock();
            LockDemo.print(null, null,getName() + " lock之后");
            System.out.println(getName() + ": running");
            Thread.sleep(1000);
            LockDemo.print(null, null,getName() + " sleep唤醒之后");
            System.out.println(getName() + ": sleep ending");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println(getName() + ": ending");
            lock.unlock();
            try {
                LockDemo.print(null, null,getName() + " unlock之后");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


