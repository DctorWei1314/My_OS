package com.system.kernel;
import com.system.UI.ColorFactory;
import com.system.UI.CpuFrame;
import com.system.UI.Init_UI;
import com.system.bufferPool.BufferBlock;
import com.system.disk.Disk;
import com.system.process.Pcb;
import com.system.process.Process;
import com.system.process.SystemCall;
import com.system.process.Task;
import sun.plugin.dom.css.RGBColor;
import sun.plugin2.gluegen.runtime.CPU;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Enumeration;
import java.util.LinkedList;

public class Kernel {
    static private int pidseed = 0;
    public static void shedule(int CPU_ID){
        Pcb pcb = Init_UI.pcbQueue.maxCountPcb(CPU_ID);
        if(pcb!=null){
            pcb.stop = false;
            synchronized(pcb.processRuning){
                pcb.processRuning.notify();
            }
        }
        else {
            Init_UI.pcbQueue.allIncreaseCount();
            pcb = Init_UI.pcbQueue.maxCountPcb(CPU_ID);
            while (pcb ==null){
                pcb = Init_UI.pcbQueue.maxCountPcb(CPU_ID);
            }
            pcb.stop = false;
            synchronized(pcb.processRuning){
                pcb.processRuning.notify();
            }
        }
    }

    public static int create_process(int father_pid,int priorty){
        Process process = new Process(++pidseed,father_pid,priorty);
        process.pcb.setThread(process);
        Init_UI.pcbQueue.addPcb(process.pcb);
        Init_UI.processeList.add(process);//便利UI线程往进程内插入动态插入任务
        JButton processButton = new JButton("<html><span color=yellow>proc"+pidseed+"</span><html>");
        processButton.setBackground(Color.black);
        CpuFrame.ProcessJpanel processJpanel = new CpuFrame.ProcessJpanel(processButton);
        Init_UI.procPanelMap.put(pidseed,processJpanel);
        Init_UI.cpuFrame.jp1.add(processJpanel);
//        Init_UI.cpuFrame.jp1.revalidate();
        if(father_pid == 0){
            Init_UI.process_root = new DefaultMutableTreeNode(process);
            Init_UI.process_tree = new JTree(Init_UI.process_root);
        }
        else {
            DefaultTreeModel model = (DefaultTreeModel) Init_UI.process_tree.getModel();
            model.insertNodeInto(new DefaultMutableTreeNode(process),visitAllNodes(Init_UI.process_root,father_pid),0);
            //使树展开
        }
        process.pcb.processRuning.start();
        return pidseed;
    }

    public static void exit_process(int pid){
        removeNode(pid);
        Init_UI.cpuFrame.jp1.remove(Init_UI.procPanelMap.get(pid));
        Process process = null;
        for(int i=0;i<Init_UI.processeList.size();i++){
            if(Init_UI.processeList.get(i).pcb.pid == pid){
                process = Init_UI.processeList.remove(i);
                break;
            }
        }
        if(process.pcb != null) {
            Init_UI.pcbQueue.rmPcb(process.pcb);
        }
    }

    public static void send(Process process){
        LinkedList<BufferBlock> msgList = splitSendMsg(process.privateSpace,process.pcb.pid);
        for(int i=0;i<Init_UI.processeList.size();i++){
            if(Init_UI.processeList.get(i).pcb.pid == process.parameter){
                LinkedList<LinkedList<BufferBlock>> msg = Init_UI.processeList.get(i).pcb.msg;
                msg.add(msgList);
                CpuFrame.ProcessJpanel processJpanel = Init_UI.procPanelMap.get(process.parameter);
                Color color = ColorFactory.makeColor(msg.size());
                for (BufferBlock bufferBlock:msgList){
                    bufferBlock.jbutton.setBackground(color);
                    Init_UI.cpuFrame.jp3.remove(bufferBlock.jbutton);
                    Init_UI.cpuFrame.jp3.revalidate();
                    processJpanel.add(bufferBlock.jbutton);
                }
                processJpanel.revalidate();
                Kernel.check_notify(Init_UI.processeList.get(i),SystemCall.receive);
                break;
            }
        }
    }
    private static LinkedList<BufferBlock> splitSendMsg(byte[] msg,int pid){
        LinkedList<BufferBlock> msgList = new LinkedList<BufferBlock>();
        int circleTime = msg.length/512;
        if(msg.length%512!=0)circleTime++;
        for(int i=0;i<circleTime;i++){
            try {
                BufferBlock bufferBlock = Init_UI.bufferPool.getone();
                msgList.add(bufferBlock);
                int end =msg.length-i*512;
                if(end>512)end=512;
                bufferBlock.realSize = end;
                System.arraycopy(msg, i*512, bufferBlock.getContent(), 0,end);
                bufferBlock.sender_pid = pid;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return msgList;
    }
    public static void check_notify(Process process,SystemCall systemCall){
        if(Init_UI.pcbQueue.isWait(process.pcb)&&process.systemCall == systemCall){
            Init_UI.pcbQueue.mv2Ready(process.pcb);
            Init_UI.procPanelMap.get(process.pcb.pid).setBackground(new Color(238,238,238));
        }
    }
    public static void receive(Process process,int cpuid){
        if(process.pcb.msg.size() == 0){
            process.addTask(new Task(SystemCall.receive,0));//唤醒后立即再次请求,不真实的模拟内核中断后恢复内核现场
            Init_UI.pcbQueue.mv2Wait(process.pcb);
            process.pcb.CPU_ID = 0;//释放cpu占用资源
            Init_UI.procPanelMap.get(process.pcb.pid).setBackground(Color.GRAY);
            shedule(cpuid);
            return;
        }
        LinkedList<BufferBlock> msgList = process.pcb.msg.remove();
        process.parameter = msgList.getFirst().sender_pid;
        process.privateSpace = joinReceiveMsg(msgList);
        CpuFrame.ProcessJpanel processJpanel = Init_UI.procPanelMap.get(process.pcb.pid);
        for (BufferBlock bufferBlock:msgList){
            processJpanel.remove(bufferBlock.jbutton);
            bufferBlock.jbutton.setBackground(Color.white);
        }
        processJpanel.revalidate();
        release(msgList);
        synchronized(process.pcb.processRuning){
            process.pcb.processRuning.notify();
        }
    }
    private static byte[] joinReceiveMsg(LinkedList<BufferBlock> msgList){
        int size=0;
        for(BufferBlock bufferBlock:msgList){
            size += bufferBlock.realSize;
        }
        byte[] result = new byte[size];
        for(int i=0;i<msgList.size();i++){
            System.arraycopy(msgList.get(i).content, 0, result, i*512,msgList.get(i).realSize);
        }
        return result;
    }
    private static void release(LinkedList<BufferBlock> msgList){
        for(BufferBlock bufferBlock:msgList){
            Init_UI.bufferPool.release(bufferBlock);
        }
    }
    public static void require_disk_read(Process process,int cpuid){
        BufferBlock bufferBlock = null;
        try {
            bufferBlock = Init_UI.bufferPool.getone();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        bufferBlock.sender_pid = process.pcb.pid;
        bufferBlock.realSize = 1;//复用该标志为读块
        Init_UI.disk.addtoIOLink(process.parameter,bufferBlock);
        Init_UI.pcbQueue.mv2Wait(process.pcb);
        process.pcb.CPU_ID = 0;//释放cpu占用资源
        Init_UI.procPanelMap.get(process.pcb.pid).setBackground(Color.GRAY);
        shedule(cpuid);
    }

    public static void require_disk_write(Process process){
        BufferBlock bufferBlock = null;
        try {
             bufferBlock = Init_UI.bufferPool.getone();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        bufferBlock.sender_pid = process.pcb.pid;
        bufferBlock.realSize = 0;//复用该标志为写块
        Init_UI.disk.addtoIOLink(process.parameter,bufferBlock);
    }
    private static DefaultMutableTreeNode visitAllNodes(DefaultMutableTreeNode node, int father_pid) {
        // node is visited exactly once
        Process process = (Process)node.getUserObject();
        if(process.pcb.pid == father_pid){
            return node;
        }
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode)e.nextElement();
                DefaultMutableTreeNode result = visitAllNodes(n,father_pid);
                if(result != null)return result;
            }
        }
        return null;
    }
    private static boolean removeNode(int pid) {
        // node is visited exactly once
//        Process process = (Process)node.getUserObject();
        DefaultMutableTreeNode defaultMutableTreeNode = visitAllNodes(Init_UI.process_root,pid);
        if(defaultMutableTreeNode==null)return false;
//        defaultMutableTreeNode.removeFromParent();
        DefaultTreeModel model = (DefaultTreeModel) Init_UI.process_tree.getModel();
//        Process process = (Process)defaultMutableTreeNode.getUserObject();
            for (Enumeration e = defaultMutableTreeNode.children(); e.hasMoreElements(); ) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode)e.nextElement();
//                n.setParent(Init_UI.process_root);
                model.insertNodeInto(n,Init_UI.process_root,0);
            }
            System.out.println(defaultMutableTreeNode.getUserObject());
            model.removeNodeFromParent(defaultMutableTreeNode);
            return true;
    }
//    static private void expandAll(JTree tree, boolean expand) {
//        TreeNode root = (TreeNode)tree.getModel().getRoot();
//
//        // Traverse tree from root
//        expandAll(tree, new TreePath(root), expand);
//    }
//    static private void expandAll(JTree tree, TreePath parent, boolean expand) {
//        // Traverse children
//        TreeNode node = (TreeNode)parent.getLastPathComponent();
//        if (node.getChildCount() >= 0) {
//            for (Enumeration e=node.children(); e.hasMoreElements(); ) {
//                TreeNode n = (TreeNode)e.nextElement();
//                TreePath path = parent.pathByAddingChild(n);
//                expandAll(tree, path, expand);
//            }
//        }
//
//        // Expansion or collapse must be done bottom-up
//        if (expand) {
//            tree.expandPath(parent);
//        } else {
//            tree.collapsePath(parent);
//        }
//    }
}
