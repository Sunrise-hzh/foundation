package com.sunrise.study.thread.utils;

import com.sunrise.study.thread.sourcecode.Condition;
import com.sunrise.study.thread.sourcecode.ReentrantLock;

import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author huangzihua
 * @date 2021-08-16
 */
public class AQSPrint {

    public static void outRRWL(String pre, Object lock) {
        try {
            Class<ReentrantReadWriteLock> lockClass = ReentrantReadWriteLock.class;

            /* 打印 */
            Field readHolds = lockClass.getDeclaredField("readHolds");
            Field cachedHoldCounter = lockClass.getDeclaredField("cachedHoldCounter");
            Field firstReader = lockClass.getDeclaredField("firstReader");
            Field firstReaderHoldCount = lockClass.getDeclaredField("firstReaderHoldCount");

            StringBuilder printStr = new StringBuilder();
            printAQSState(printStr, lock, lockClass);
            System.out.println(pre + ": " + printStr);
        } catch (Exception e) {

        }

    }

    private static void printAQSState(StringBuilder printStr, Object lock, Class lockClass) {
        try {
            Field sync = lockClass.getDeclaredField("sync");
            sync.setAccessible(true);
            Object syncObject = sync.get(lock);
            Class<?> nonfairSyncClass = syncObject.getClass();
            Class<?> syncClass = nonfairSyncClass.getSuperclass();
            Class<?> aqsClass = syncClass.getSuperclass();
            Field[] aqsFields = aqsClass.getDeclaredFields();

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void outRL(Condition addCondition, Condition eatCondition, String pre, Object lock) throws NoSuchFieldException, IllegalAccessException {
        Class<ReentrantLock> lockClass = ReentrantLock.class;
        StringBuilder printStr = new StringBuilder();

        printAQSState(printStr, lock, lockClass);

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
