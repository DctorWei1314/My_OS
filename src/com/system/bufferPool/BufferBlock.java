package com.system.bufferPool;

import javax.swing.*;

public class BufferBlock {
    public int realSize;
    public int sender_pid;
    public byte[] content;
    public JButton jbutton;
    void setJbutton(JButton jbutton){
        this.jbutton = jbutton;
    }

    BufferBlock() {
        content = new byte[512];
    }

    BufferBlock(int size){
        content = new byte[size];
    }

    public byte[] getContent(){
        return content;
    }

    byte[] read(){
        return subBytes(content,0,realSize);
    }

    private byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        for (int i=begin; i<begin+count; i++) bs[i-begin] = src[i];
        return bs;
    }
}
