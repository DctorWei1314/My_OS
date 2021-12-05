package com.system.process;

import com.system.bufferPool.BufferBlock;
import sun.awt.Mutex;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class Pcb {
    public int CPU_ID = 0;
    //public String name;
    public int pid;
    public int priority;
    public int father_pid;
    public Thread processRuning;
    public boolean stop = true;
    public boolean exit = false;
//    public boolean state = true;//true表示就绪，false表示等待
    //CPUwithTimer用stop来控制Process,让其被动暂停运行，时间片剥夺
    public int livingTime;
    public int count = 0;//结合优先级与时间片的高效调度
    public LinkedList<LinkedList<BufferBlock>> msg = new LinkedList<LinkedList<BufferBlock>>();
    public Mutex msgMutex = new Mutex();
    public Pcb(int pid ,int father_pid,int priority0){
        this.pid = pid;
        this.father_pid = father_pid;
        if(priority0<=5)
            priority0=5;
        if(priority0>=10)
            priority0=10;
        this.priority = priority0;
    }
    public void setThread(Process process){
        processRuning = new Thread(process);
    }
}
