package com.system.UI;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class CpuFrame extends JFrame {
    public static class ProcessJpanel extends JPanel{
        public JButton jButton;
        public ProcessJpanel(JButton jButton){
            this.jButton = jButton;
            add(jButton);
            setVisible(true);
            setBorder(BorderFactory.createLoweredBevelBorder());
        }
    }
    public JPanel jp1,jp2,jp3;
    CpuFrame(String title,int cpu){
        super(title);
        jp1=new JPanel();	//创建面板
        jp2=new JPanel();
        jp3=new JPanel();
        this.setLayout(new BorderLayout());	// 设置窗体的布局方式
        add(jp2,BorderLayout.NORTH);
        add(jp1,BorderLayout.CENTER);
        add(jp3,BorderLayout.SOUTH);
        add(Init_UI.disk.diskPanel,BorderLayout.EAST);
        for(int i=1;i<=cpu;i++){
            JButton jButton = new JButton("CPU"+i);
            jButton.setBackground(ColorFactory.makeColor(i));
            jp2.add(jButton);
        }
        setVisible(true);
        setBounds(500,500,1800, 550);
    }
}
