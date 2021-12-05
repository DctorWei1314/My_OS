package com.system.process;


import com.system.UI.Init_UI;

import java.util.LinkedList;

/*
需要一个全局的公共进程队列，以便多个CPUwithTimer线程调度
*/
public class PcbQueue {
    public LinkedList<Pcb> PcbReadyList;//就绪队列
    public LinkedList<Pcb> PcbWaitList;//等待队列
    public PcbQueue(){
        PcbReadyList = new LinkedList<Pcb>();
        PcbWaitList = new LinkedList<Pcb>();
    }
    public synchronized void addPcb(Pcb pcb){
        PcbReadyList.add(pcb);
    }
    public synchronized void rmPcb(Pcb pcb){
        PcbReadyList.remove(pcb);
    }
    public synchronized void mv2Wait(Pcb pcb){
        PcbReadyList.remove(pcb);
        PcbWaitList.add(pcb);
    }
    public synchronized void mv2Ready(Pcb pcb){
        PcbWaitList.remove(pcb);
        PcbReadyList.add(pcb);
    }
    public synchronized Pcb maxCountPcb(int CPU_ID){
        int temp = 0;
        Pcb result = null;
        for (Pcb pcb:PcbReadyList
             ) {
            if(pcb.count>temp&&pcb.CPU_ID==0){
                temp = pcb.count;
                result = pcb;
            }
        }
        if(result != null){
            result.CPU_ID =CPU_ID;
            Init_UI.CUP_List.get(CPU_ID-1).runnigProcess = result.pid;
        }
        return result;//如果就绪队列里的进程都为0，则返回空指针
    }
    public synchronized void allIncreaseCount(){
        for (Pcb pcb:PcbReadyList
        ) {
            if(pcb.CPU_ID==0)
            pcb.count = pcb.priority;
        }
        for (Pcb pcb:PcbWaitList
        ) {
            pcb.count = pcb.count/2+pcb.priority;
        }
    }
    public synchronized boolean isWait(Pcb pcb){
        return PcbWaitList.contains(pcb);
    }
}
