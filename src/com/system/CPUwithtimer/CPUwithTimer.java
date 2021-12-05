package com.system.CPUwithtimer;

import com.system.UI.Init_UI;
import com.system.kernel.Kernel;
import com.system.process.Pcb;
import com.system.process.Process;
import com.system.process.SystemCall;
import org.omg.CORBA.INTERNAL;

import java.awt.*;

import static java.lang.Thread.sleep;

//模拟带有时间中断的CPU
/*
* 每一个进程和CPUwithTimer线程实际上是在不同的线程中运行，但是他们是同步的，CPUwithTimer和它所调度的进程所在的实际执行线程不能同时运行，以此模拟单个cpu资源
* 当CPUwithTimer选中调度的进程运行，它所在的线程先唤醒调度的进程所在的实际线程，然后sleep一段间隔，这段间隔模拟时间片中断，正常睡醒后执行时间中断处理程序，循环往复
* 而若所调动的进程在CPUwithTimer线程sleep期间主动interrupt打断CPUwithTimer睡眠后，它本身接着立刻阻塞，而CPUwithTimer线程则进入interrupt异常的处理程序，模拟系统调用的过程
* interrupt异常的处理程序根据进程私有空间的参数，确定系统调用的类型，及参数
* */
public class CPUwithTimer implements Runnable{
    int CPU_ID;
    int lastRequsetPid;
    Interrupter extend_interrupt_object = null;
    public int runnigProcess;
    public Color color;
    public CPUwithTimer(int CPU_ID,Color color){
        this.CPU_ID = CPU_ID;
        this.color = color;
    }

    //模拟带有时间中断的CPU的运行过程
    @Override
    public void run() {
        Kernel.shedule(CPU_ID);
        while (true){
//            System.out.println("CUPruning");
            try {
                sleep(100);
            } catch (InterruptedException e) {
                dealSystemCall();
                continue;
            }
            if(extend_interrupt_object!=null){
                extend_interrupt_object.deal_interrupt();
                extend_interrupt_object = null;
                continue;
            }
            Pcb pcb = runingProess();
            pcb.stop = true;
            pcb.livingTime++;
            pcb.count--;
            if(pcb.count <= 0){
                pcb.CPU_ID = 0;//释放cpu占用资源
                Kernel.shedule(CPU_ID);//如果时间片用完了,调度进程
                continue;
            }
            //否则继续调度该进程
            pcb.stop = false;
            synchronized(pcb.processRuning){
                pcb.processRuning.notify();
            }
        }
    }

    public void setLastRequsetPid(int pid){
        lastRequsetPid = pid;
    }
    private void dealSystemCall(){
        Process process = null;
        for(int i=0;i<Init_UI.processeList.size();i++){
            if(Init_UI.processeList.get(i).pcb.pid == lastRequsetPid){
                process = Init_UI.processeList.get(i);
                break;
            }
        }
        if(process != null){
            if(process.systemCall == SystemCall.create_process) {
                Kernel.create_process(process.pcb.pid,process.parameter);
                synchronized(process.pcb.processRuning){
                    process.pcb.processRuning.notify();
                }
            }
            else if(process.systemCall == SystemCall.exit_process){
                Kernel.exit_process(process.pcb.pid);
                Kernel.shedule(CPU_ID);
            }
            else if(process.systemCall == SystemCall.send){
                Kernel.send(process);
                synchronized(process.pcb.processRuning){
                    process.pcb.processRuning.notify();
                }
            }
            else if(process.systemCall == SystemCall.receive){
                Kernel.receive(process,this.CPU_ID);
            }
            else if(process.systemCall == SystemCall.require_disk_read){
                Kernel.require_disk_read(process,this.CPU_ID);
            }
            else if(process.systemCall == SystemCall.require_disk_write){
                Kernel.require_disk_write(process);
                synchronized(process.pcb.processRuning){
                    process.pcb.processRuning.notify();
                }
            }
        }
    }
    private Pcb runingProess(){
        Process process = null;
        for(int i=0;i<Init_UI.processeList.size();i++)
            if(Init_UI.processeList.get(i).pcb.pid == runnigProcess){
                process = Init_UI.processeList.get(i);
                return process.pcb;
        }
        return null;
    }

    public void setExtend_interrupt_object(Interrupter extend_interrupt_object) {
        this.extend_interrupt_object = extend_interrupt_object;
    }
}
