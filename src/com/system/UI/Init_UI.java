package com.system.UI;

import com.system.CPUwithtimer.CPUwithTimer;
import com.system.bufferPool.BufferPool;
import com.system.disk.Disk;
import com.system.kernel.Kernel;
import com.system.process.*;
import com.system.process.Process;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class Init_UI {
    public static Disk disk = new Disk(200);
    public static BufferPool bufferPool;
    public static PcbQueue pcbQueue = new PcbQueue();
    public static LinkedList<Process> processeList = new LinkedList<>();
    //便于添加任务
    public static DefaultMutableTreeNode process_root;
    public static JTree process_tree;
    //进程树
    public static LinkedList<CPUwithTimer> CUP_List= new  LinkedList<>();
    public static LinkedList<Thread> CUPRUNNIG_List= new  LinkedList<>();
    public static Map<Integer,CpuFrame.ProcessJpanel> procPanelMap = new HashMap<Integer,CpuFrame.ProcessJpanel>();
    public static CpuFrame cpuFrame;
    public static void main(String[] args) throws InterruptedException {
	// write your code here
        init(2,10);
        UI ui = new UI();
        ui.UI_Woring();
        System.out.println("123");
    }
    //提供初始化多个CPU的模拟线程资源
    //支持多核CUP功能
    /*主线程负责
    1.监听运行状态,实时传输UI数据,并根据UI反馈实时动态给进程添加执行任务
    2.负责创建第一个进程，以便CPUwithTimer调度，
    （如果有多个CPUwithTimer，而初始进程只有一个，其余CPUwithTimer没有process可以调度则会等待，直到初始进程产生子进程后其余CPUwithTimer可以正常工作）
    并产生更多进程,其他进程均为该进程的子辈进程，但是其他进程的创建不由主线程负责，
    而是由在随机的CPUwithTimer线程里运行的process调用create_process()系统调用完成
    3.负责CPUwithTimer线程的创建
    */
    //注：init函数负责的正是主线程的2,3功能，且完成缓冲池资源的初始化
    static void init(int CPUNUM,int poolNum){
        Init_UI.cpuFrame =new CpuFrame("缓冲与调度",CPUNUM);
        bufferPool = new BufferPool(poolNum);
        Kernel.create_process(0,10);
        CPUwithTimer CPU;
        for(int i=1;i<=CPUNUM;i++){
            CPU = new CPUwithTimer(i,ColorFactory.makeColor(i));
            Thread CPURUNNIG = new Thread(CPU);
            CUP_List.add(CPU);
            CUPRUNNIG_List.add(CPURUNNIG);
            CPURUNNIG.start();
        }
    }
}
class UI{
    MyFrame window;
    //注：UI_Woring函数负责的正是主线程的1功能
    void UI_Woring(){
        window = new MyFrame("进程树与调用控制");
        while (true){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            window.repaint();
            for (CpuFrame.ProcessJpanel value : Init_UI.procPanelMap.values()) {
                value.jButton.setBackground(Color.black);
            }
            for(CPUwithTimer cpu:Init_UI.CUP_List){
                CpuFrame.ProcessJpanel item = Init_UI.procPanelMap.get(cpu.runnigProcess);
                if(item == null) continue;
                else item.jButton.setBackground(cpu.color);
            }
            Init_UI.cpuFrame.jp1.revalidate();
            if(Init_UI.disk.lifetime++==Init_UI.disk.IOtime&&Init_UI.disk.busy){
                Init_UI.CUP_List.get(0).setExtend_interrupt_object(Init_UI.disk);
            }
        }
    }
}
class MyFrame extends JFrame{
    MyFrame(String title){
        super(title);
        setVisible(true);
        setBounds(100,100,400,400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Container container = getContentPane();

        Init_UI.process_tree.addTreeSelectionListener(new TreeSelectionListener(){

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) Init_UI.process_tree
                        .getLastSelectedPathComponent();
                if(node!=null){
                    Process process = (Process)node.getUserObject();
                    new MyDialog(process);
                }
            }
        });
        container.add(Init_UI.process_tree);
    }

}
class MyDialog extends JDialog{
    JPanel jp1,jp2,jp3;
    JLabel jlb1,jlb2;
    JButton jb1;
    JRadioButton jrb1,jrb2,jrb3,jrb4,jrb5,jrb6;
    ButtonGroup bg;
    Process process;
    public MyDialog(Process process) {
        setTitle("process:"+process.pcb.pid);
        jp1=new JPanel();	//创建面板
        jp2=new JPanel();
        jp3=new JPanel();
        jb1=new JButton("确认授予系统调用任务");		//创建按钮
        jlb1=new JLabel("调用类型:");	//创建标签
        jlb2=new JLabel("参数:");
        jrb1=new JRadioButton("create_process");			//创建单选框 ,,,
        jrb2=new JRadioButton("exit_process");
        jrb3=new JRadioButton("send");
        jrb4=new JRadioButton("receive");
        jrb5=new JRadioButton("require_disk_read");
        jrb6=new JRadioButton("require_disk_write");
        bg=new ButtonGroup();				//创建按钮组
        this.setLayout(new GridLayout(3,1));	//三行一列网格布局
        this.add(jp1);			//添加三个面板
        this.add(jp2);
        this.add(jp3);
        jp1.add(jlb1);			//添加面板1的组件
        bg.add(jrb1);			//必须要把单选框放入按钮组作用域中才能实现单选！！！！
        bg.add(jrb2);
        bg.add(jrb3);
        bg.add(jrb4);
        bg.add(jrb5);
        bg.add(jrb6);
        jp1.add(jrb1);
        jp1.add(jrb2);
        jp1.add(jrb3);
        jp1.add(jrb4);
        jp1.add(jrb5);
        jp1.add(jrb6);

        jp2.add(jlb2);			//添加面板2的组件
        JTextArea jtextarea = new JTextArea(3, 10); // 设置大小
        //在有带滚动条的面板中设置文本域，并设置垂直滚动条，垂直滚动条。
        JScrollPane scr = new JScrollPane(jtextarea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jp2.add(scr);

        jp3.add(jb1);

        this.process = process;
        setVisible(true);
        setBounds(100,100,400, 550);
//        Container container = getContentPane();
//        container.setLayout(null);
        jb1.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                Task task = null;
                int time = 0;
                String text = jtextarea.getText();
                if(jrb1.isSelected()){
                    String[] strings = text.split("\n",2);
                    if(strings.length>0&&!strings[0].equals(""))time = Integer.parseInt(strings[0]);
                    if(strings.length>1&&!strings[1].equals(""))
                    {
                        int priority = Integer.parseInt(strings[1]);
                        task = new Task(SystemCall.create_process,time,priority);
                    }
                    else
                        task = new Task(SystemCall.create_process,time);
                }
                else if(jrb2.isSelected()){
                    String[] strings = text.split("\n",2);
                    if(strings.length>0&&!strings[0].equals(""))time = Integer.parseInt(strings[0]);
                    task = new Task(SystemCall.exit_process,time);
                }
                else if(jrb3.isSelected()){
                    int pid = 1;
                    String[] strings = text.split("\n",3);
                    if(strings.length>0&&!strings[0].equals(""))time = Integer.parseInt(strings[0]);
                    if(strings.length>1&&!strings[1].equals("")) pid = Integer.parseInt(strings[1]);
                    byte[] content =null;
                    if(strings.length>2&&!strings[2].equals("")) content =strings[2].getBytes();
//                    byte[] content =strings[2].getBytes();
                    task = new Task(SystemCall.send,time,pid,content);
                }
                else if(jrb4.isSelected()){
                    String[] strings = text.split("\n",2);
                    if(strings.length>0&&!strings[0].equals(""))time = Integer.parseInt(strings[0]);
                    task = new Task(SystemCall.receive,time);
                }else if(jrb5.isSelected()){
                    String[] strings = text.split("\n",2);
                    if(strings.length>0&&!strings[0].equals(""))time = Integer.parseInt(strings[0]);
                    int require_number;
                    if(strings.length>1&&!strings[1].equals(""))
                    {
                        require_number = Integer.parseInt(strings[1]);
                        task = new Task(SystemCall.require_disk_read,time,require_number);
                    }
                }else if(jrb6.isSelected()){
                    String[] strings = text.split("\n",2);
                    if(strings.length>0&&!strings[0].equals(""))time = Integer.parseInt(strings[0]);
                    int require_number;
                    if(strings.length>1&&!strings[1].equals(""))
                    {
                        require_number = Integer.parseInt(strings[1]);
                        task = new Task(SystemCall.require_disk_write,time,require_number);
                    }
                }
                if(task != null)
                    process.addTask(task);
            }
        });
    }
}

