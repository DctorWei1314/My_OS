package com.system.bufferPool;
import com.system.UI.CpuFrame;
import com.system.UI.Init_UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class BufferPool {
    int bufferNum;
    Semaphore bufferSemaphore;
    LinkedList<BufferBlock> BufferBlockList = new LinkedList<BufferBlock>();
    public BufferPool(int size){
        bufferNum = size;
        bufferSemaphore = new Semaphore(size);
        for(int i=0;i<size;i++){
            BufferBlock temp = new BufferBlock();
            BufferBlockList.add(temp);
            JButton button = new JButton("buf"+i);
            Dimension preferredSize = new Dimension(60,60);
            button.setPreferredSize(preferredSize );
            button.setBackground(Color.white);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                }
            });
            temp.setJbutton(button);
            Init_UI.cpuFrame.jp3.add(button);
        }
    }

    public synchronized BufferBlock getone() throws InterruptedException {
        bufferSemaphore.acquire() ;
        BufferBlock bufferBlock = BufferBlockList.removeFirst();
        Init_UI.cpuFrame.jp3.remove(bufferBlock.jbutton);
        return bufferBlock;
    }

    public synchronized void release(BufferBlock bufferBlock){
        BufferBlockList.add(bufferBlock);
        Init_UI.cpuFrame.jp3.add(bufferBlock.jbutton);
        bufferSemaphore.release();
    }
    int leftBlockNum(){
        return BufferBlockList.size();
    }
}
