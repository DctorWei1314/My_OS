package com.system.disk;

import com.system.CPUwithtimer.Interrupter;
import com.system.UI.ColorFactory;
import com.system.UI.CpuFrame;
import com.system.UI.Init_UI;
import com.system.bufferPool.BufferBlock;
import com.system.kernel.Kernel;
import com.system.process.SystemCall;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;

public class Disk implements Interrupter {

    public class Node{
        BufferBlock bufferBlock;
        Node nextnode;
        Node(BufferBlock bufferBlock){
            this.bufferBlock = bufferBlock;
            nextnode = null;
        }

        private void setNextnode(Node nextnode) {
            this.nextnode = nextnode;
        }
        public void addNextnode(Node nextnode){
            Node node = this;
            while (node.nextnode!=null){
                node = node.nextnode;
            }
            node.nextnode = nextnode;
        }
    }
    public JPanel[] jPanels = new JPanel[100];
    public JPanel diskPanel = new JPanel();
    public int IOtime;
    public int lifetime = 0;
    public boolean busy = false;
    int headpos = 0;
    boolean direction;//true up false down
    Node[] count = new Node[100];

    public Disk(int time){
        this.IOtime = time;
        for(int i=0;i<100;i++){
            jPanels[i] = new JPanel();
            jPanels[i].setBorder(BorderFactory.createCompoundBorder());
            jPanels[i].setVisible(true);
            jPanels[i].setSize(20,20);
            jPanels[i].setBackground(Color.orange);
//            JButton button = new JButton(""+i);
//            Dimension preferredSize = new Dimension(10,20);
//            button.setPreferredSize(preferredSize );
//            jPanels[i].add(button);
            diskPanel.add(jPanels[i]);
        }
        diskPanel.setVisible(true);
//        diskPanel.setSize(20,20);
    }
    synchronized public void addtoIOLink(int position, BufferBlock block){
            if(Init_UI.disk.busy){
                if(Init_UI.disk.count[position]==null)
                    Init_UI.disk.count[position] = new Disk.Node(block);
                else Init_UI.disk.count[position].addNextnode(new Disk.Node(block));
            }else {
                busy = true;
                lifetime = 0;
                count[position] = new Disk.Node(block);
                if(position < headpos){
                    direction = false;
                }else{
                    direction = true;
                }
                headpos = position%100;
            }
        jPanels[position%100].add(block.jbutton);
    }
    synchronized public void deal_interrupt(){
        BufferBlock bufferBlock = count[headpos].bufferBlock;
        jPanels[headpos].remove(bufferBlock.jbutton);
        Init_UI.cpuFrame.jp3.add(bufferBlock.jbutton);
        if(bufferBlock.realSize != 0){
            for(int i = 0; i< Init_UI.processeList.size(); i++){
                if(Init_UI.processeList.get(i).pcb.pid == bufferBlock.sender_pid){
                    Kernel.check_notify(Init_UI.processeList.get(i),SystemCall.require_disk_read);
                    break;
                }
            }
        }
        count[headpos] = count[headpos].nextnode;
        busy = false;
        if(direction){
            if(upscan())
                return;
            downscan();
        }else {
            if(downscan())
                return;
            upscan();
        }
    }
    boolean upscan(){
        int i = headpos;
        while (i<=99&&count[i]==null){
            i++;
        }
        if(i<=99){
            busy = true;
            headpos = i;
            lifetime = 0;
            return true;
        }
        return false;
    }
    boolean downscan(){
        int i = headpos;
        while (i>=0&&count[i]==null){
            i--;
        }
        if(i>=0){
            busy = true;
            headpos = i;
            lifetime = 0;
            return true;
        }
        return false;
    }
}
