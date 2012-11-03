
package org.alljoyn.bus.samples.simpleclient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//class fb_user
//{
//	String username;
//	int contribution;
//	public fb_user(String name)
//	{
//		username=name;
//		contribution=0;
//	}
//}

public class Client extends Activity {
	/* Load the native alljoyn_java library. */
	static {
		System.loadLibrary("alljoyn_java");
	}
	//max value of capacity is 16384.
	//int no_of_times=37;
	
	//for facebook hackathon
	public volatile int timer_flag=0;
	public int total_packets=0;
	public ArrayList<String> participants=new ArrayList<String>();
	public ListView fbListView;
	int no_of_times=1;
	int capacity=16384;
    private SimpleService mSimpleService;
	long average=0;
	public Hashtable<Integer, String> facebook_names=new Hashtable<Integer, String>();
    /************************************************************************* server code ***************************************************************************/
	    
    class SimpleService implements SimpleInterface, BusObject {

    	public int[] pixels,smoothed_pixels;
    	public int no_of_clients_needed,no_of_times_client_calls_image_data,size_of_each_chunk,no_of_clients=3,current_client_id=-1;
    	public int chunks_recd=0;//to keep track of when all chunks have been returned
    	public int server_done=0;//to keep track of whether or not the server thread has finished smoothing
    	//no_of_clients simply lists the no. of clients. server is not included in that number!
    	double[][] decisiontable=new double[1000][];//random number 100. needs to be refined
    	int[] frequencies;
    	double fmax;
    	long timebefore, timeafter;
    	int index;
    	public int w,h;
		Hashtable<Integer,Double> uvmv=new Hashtable<Integer,Double>();
						 
		public SimpleService()
		{
			//setImageView("/storage/sdcard0/received_image.png");
			facebook_names.put(0, "Manoj Krishnan");
			facebook_names.put(1, "Vishnu Nair");
			facebook_names.put(2, "Harshit Kharbanda");
			timebefore=System.currentTimeMillis();
			preProcessImage();
			no_of_clients_needed=core_algo()-1;//because core_algo returns number of devices that need to be used, not clients
			//no_of_clients_needed=no_of_clients;//facebook hacking
			//so right now, preprocess is done and core_algo is also done. start the server thread now
			
			//start server processing thread here
			new Thread(new Runnable() { 
		        public void run() { 
		        	
		        	double x=decisiontable[index][2]/fmax;
					int serverstartindex=(int)(pixels.length*(1-x));					
		        	
		        	System.arraycopy(pixels,serverstartindex,smoothed_pixels,serverstartindex,pixels.length-serverstartindex);
			    	//loop to do smoothing
		        	//for(int e=0;e<12;e++)
		        	//{
					for(int k=0;k<50;k++)
					{
						System.out.println("serverside iteration number "+k);
						for(int i=serverstartindex+1;i<pixels.length-1;i++)
						{
							 int a0=Color.alpha(smoothed_pixels[i-1]);
							 int r0=Color.red(smoothed_pixels[i-1]);
							 int g0=Color.green(smoothed_pixels[i-1]);
							 int b0=Color.blue(smoothed_pixels[i-1]);
							 int a=Color.alpha(smoothed_pixels[i]);
							 int r=Color.red(smoothed_pixels[i]);
							 int g=Color.green(smoothed_pixels[i]);
							 int b=Color.blue(smoothed_pixels[i]);
							 int a1=Color.alpha(smoothed_pixels[i+1]);
							 int r1=Color.red(smoothed_pixels[i+1]);
							 int g1=Color.green(smoothed_pixels[i+1]);
							 int b1=Color.blue(smoothed_pixels[i+1]);							 
							 int afinal=(a0+a+a1)/3;
							 int rfinal=(r0+r+r1)/3;
							 int gfinal=(g0+g+g1)/3;
							 int bfinal=(b0+b+b1)/3;
							 
							 smoothed_pixels[i]=Color.argb(afinal,rfinal,gfinal,bfinal);							 
						}
						//the following block was added for the facebook hackathon
						if(k%5==0)
						{
							//the following lines added for facebook hackathon
							Bitmap image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
					    	  
						    for (int i = 0; i < h; i++) 
						    {
						        for (int j = 0; j < w; j++) 
						        {			          
						        	image.setPixel(j, i, smoothed_pixels[i*w+j]);		        		
						        }
						     }
						    
						    //FileOutputStream fout=new FileOutputStream("/storage/sdcard0/received_image.png");
						    try{
						    if(completelyZero(smoothed_pixels)!=1)
						    {
						    FileOutputStream fout=new FileOutputStream("/storage/sdcard0/received_image.png");
						    image.compress(Bitmap.CompressFormat.PNG, 100, fout);
						    fout.flush();
						    fout.close();
						    }
						    }
						    catch(Exception e)
						    {
						    	e.printStackTrace();
						    }
						}
					}
		        	//}
					server_done=1;//important to set server_done to 1 to signal that server has finished its portion of smoothing.
					
		        } 
				}).start(); 
					
			System.out.println("no of clients needed is "+no_of_clients_needed);
		
		}
		public int completelyZero(int[] array)
		{
			for(int i=0;i<array.length;i++)
			{
				if(array[i]!=0)
					return 0;
			}
			return 1;
		}
		public void setServerFrequency(int freq)
		{
			try
			{
			System.out.println(" ");
			System.out.println("Setting server freq to "+freq);
			System.out.println(" ");
			String f=(new Integer(freq)).toString();
			String min="echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
			String max="echo 1400000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
			String temp="echo "+f+" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_setspeed";
			
			Process p;
			p = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(p.getOutputStream()); 
			os.writeBytes("echo userspace > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"+"\n");
			os.writeBytes(max+"\n");
			os.writeBytes(min+"\n");
			os.writeBytes(temp+"\n");
			os.writeBytes("exit\n");
			os.flush();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		public double getFrequencyToRunAt()//frequency at which client should run at
		{
			return decisiontable[index][1];
		}
		public String Ping(double[] inStr, String ID) 
		{
        	 System.out.println(ID+ "DONE!!!DONE!!!DONE!!!DONE!!!DONE!!!DONE!!!DONE!!!DONE!!!DONE!!!DONE!!!DONE!!!");
             return "hello";
        }        
		public double getClosestFrequency(double f)//returns the closest available frequency in the list of frequencies for given value
		{
			int i=0;
			for(i=frequencies.length-3;i>=0;i--)//start at length-4 because we wanna remove the 100 and 200 mhz options from the list.
				if(frequencies[i]>f)
					return frequencies[i];
			return -1;
		}
        public int core_algo()//decides whether to run stuff locally or in a distributed manner
        //returns the value of best NUMBER OF DEVICES to be used. NOT NUMBER OF CLIENTS
        {
        	
        	//first get all possible frequencies available in the device
        	try{
        	//FileInputStream fin=new FileInputStream("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies");
        	//FileInputStream finv=new FileInputStream("/sys/devices/system/cpu/cpu0/cpufreq/UV_mV_table");
        	FileInputStream fin=new FileInputStream("/storage/sdcard0/scaling_available_frequencies");
            FileInputStream finv=new FileInputStream("/storage/sdcard0/uvmv_table");
            	
        	BufferedReader br=new BufferedReader(new InputStreamReader(fin));
        	BufferedReader brv=new BufferedReader(new InputStreamReader(finv));
        	String line=br.readLine();
        	String linev=new String("");
        	StringTokenizer data=new StringTokenizer(line," ");
        	double c=0.0055;        
        	frequencies=new int[data.countTokens()];
        	int i=0;
        	while(data.hasMoreElements())
        	{
        		frequencies[i++]=Integer.parseInt(data.nextToken().toString());        		
        	}
        	//now read from uvmv table
        	while((linev=brv.readLine())!=null)
        	{
        		StringTokenizer stv=new StringTokenizer(linev,":");
        		//now split stv[0] with the letter m as the splitter because it's usually like 1400mhz: 1450 mv
        		StringTokenizer stv0=new StringTokenizer(stv.nextElement().toString(),"m");
        		int uvmv_frequency=Integer.parseInt(stv0.nextElement().toString());
        		StringTokenizer stv1=new StringTokenizer(stv.nextElement().toString()," ");
        		int uvmv_voltage=Integer.parseInt(stv1.nextElement().toString());
        		uvmv.put(new Integer(uvmv_frequency), new Double(uvmv_voltage));
        		System.out.println(uvmv_frequency+":"+uvmv_voltage);
        	}
        	fmax=frequencies[0];//because in these kernels, the frequencies are listed in descending order
        	
        	double D=pixels.length*4/Math.pow(2,20);
        	
        	//now iterate over all the frequency steps that the server has        	
        	int dt=0;//for decisiontable        	
        	for(int ctr=1;ctr<frequencies.length-2;ctr++)//starting at ctr=1 because ctr=0 implies no clients and only server
        	{
        		double f=frequencies[ctr];
        		
        		for(int n=2;n<=no_of_clients+1;n++)//here n is for number of devices
        		{
        			double x=(f/fmax);
        			System.out.println("x is "+x);
        			double fclient=getClosestFrequency((1-x)*fmax/(n-1));//assuming that all servers and clients run same kernel, fmax and fmaxclient are the same
        			//fmax and f and fclient are in hertz
        			System.out.println("fclient is "+fclient);
        			double Ec=(1-x)*D*((n+1)+1.989);
        			double Tc=(2060 + 2028*(1-x)*D)/Math.tanh(n - 1.122);
        			
        			double latencydiff=(x*D/f) + Tc + ((1-x)*D/fclient)-(D/fmax);
        		        			
        			double ediff=(c*Math.pow(uvmv.get((int)f/1000)/1000, 2)*x*D*Math.pow(2,20))/150 
        							+ Ec 
        							+ ((1-x)*D*Math.pow(2,20)*c*Math.pow(uvmv.get((int)fclient/1000)/1000,2))/150
        							-(D*Math.pow(2,20)*c*Math.pow(uvmv.get((int)fmax/1000)/1000, 2))/150;
			
        			//dividing by 1000 in the uvmv.get statements because they are expressed in mhz in the uvmv table.
        			double ediffratio=ediff/(D*Math.pow(2,20)*c*Math.pow(uvmv.get((int)fmax/1000)/1000, 2));
        			double ldiffratio=latencydiff/(D/fmax);
        			System.out.println("ediff is "+ediff+" ad Ec is "+Ec+" and ediffratio is "+ediffratio+" and latency diff is "+latencydiff+" and ldiffratio is "+ldiffratio);
        		
        			if(ediff<0)//means that energy has been saved
        			{        				
        				decisiontable[dt]=new double[8];
        				decisiontable[dt][0]=n;
        				decisiontable[dt][1]=fclient;
        				decisiontable[dt][2]=f;        				
        				decisiontable[dt][3]=latencydiff;
        				decisiontable[dt][4]=ediff;
        				decisiontable[dt][5]=ediffratio;
        				decisiontable[dt][6]=ldiffratio;
        				decisiontable[dt++][7]=x;        				
        				System.out.println(n+" devices entry made in dt ");
        				System.out.println(" ");
            			//System.out.println(" ");
        			}
        		}
        	}
        	
        	//now the loop is over. iterate over decisiontable and choose such that energy is minimized.
        	double min=decisiontable[0][4];
        	
        	for(int j=0;j<dt;j++)
        	{	
        		//System.out.println("The ediff value is "+decisiontable[j][4]);
        		if(decisiontable[j][4]<min)
        			{
        				min=decisiontable[j][4];
        				index=j;        				
        			}
        	}
        	        	        	
        	//facebook hackathon changes        	
        	decisiontable[dt]=new double[8];
			decisiontable[dt][0]=no_of_clients;
			decisiontable[dt][1]=800;
			decisiontable[dt][2]=1000;        				
			decisiontable[dt][3]=decisiontable[index][3];
			decisiontable[dt][4]=decisiontable[index][4];
			decisiontable[dt][5]=decisiontable[index][5];
			decisiontable[dt][6]=decisiontable[index][6];
			decisiontable[dt][7]=1000/fmax;   
        	index=dt++; 	
        	
        	//now index stores the configuration that we need to use
        	System.out.println("Winning x value is "+decisiontable[index][7]+" and ediff is "+decisiontable[index][4]+"The client is being set to"+decisiontable[index][1]);     	
        	
        	//now set server frequency
        	//setServerFrequency((int)getClosestFrequency(decisiontable[index][2]));//commented out for facebook hackathon
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        	return  (int)(decisiontable[index][0]);
        	
        }
        
        
		public double[] giveData(double[] inputArray) throws BusException 
		{
			double[] array=new double[capacity];
			for(int t=0;t<capacity;t++)							
			array[t]=(int) (Math.random()*10);	
			return array;
		}
		
		public int getClientID() throws BusException
		{
			current_client_id++;
			participants.add(facebook_names.get(current_client_id));
			if(current_client_id==0)
			    {
					starttime=System.currentTimeMillis();
					timer_flag=1;
			    }

			//TextView t=(TextView)findViewById(R.id.textView1);
			//t.append(facebook_names.get(current_client_id));
			if(current_client_id<no_of_clients_needed)
				return current_client_id;
			else
				return -1;
			
		}
		
		//at the end of this function, the rgbvalues array will be filled with the proper image values
		public void preProcessImage()

		{
			
			try{
			
			Bitmap img = BitmapFactory.decodeFile("/storage/sdcard0/lenna.jpg");
			//Bitmap img = BitmapFactory.decodeFile("/mnt/sdcard/house.jpg");
			w=img.getWidth();
		    h=img.getHeight();
		    smoothed_pixels=new int[h*w];	
		    pixels=new int[h*w];
		    for (int i = 0; i < h; i++) 
		    {
		        for (int j = 0; j < w; j++) 
		        {			          
		        	  int pixel=img.getPixel(j, i);
		        	  pixels[i*w+j]=pixel;
		        	  
		        }		        
		    }
		    
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	
		public int giveImageData() throws BusException {
				
		   //16384 is the maximum no. of values that can be sent in one go.
			double x=decisiontable[index][2]/fmax;
			
           size_of_each_chunk=32768;//was initially 16000
		   no_of_times_client_calls_image_data=(int) Math.ceil((h*w*(1-x))/(size_of_each_chunk*(no_of_clients_needed)));
		   return no_of_times_client_calls_image_data;
		}

		public int[] giveActualImageData(int clientid,int datachunk) throws BusException {
			
			if(clientid!=-1)
			{
				int[] localarray=new int[size_of_each_chunk];			
				double x=decisiontable[index][2]/fmax;
				int serverstartindex=(int)(pixels.length*(1-x));
				//System.out.println("serverstartindex is "+ serverstartindex);
				System.arraycopy(pixels,(clientid*serverstartindex/(no_of_clients_needed))+(datachunk*size_of_each_chunk), localarray, 0, size_of_each_chunk);
				return localarray;
			}
			else
			{
				return null;
			}
		}
		
		public void takeImageData(int[] array, int clientid, int datachunk) throws BusException 
		{
			if(clientid!=-1)
			{
				total_packets++;
			try
			{
				//System.out.println("Received chunk "+datachunk);
				double x=decisiontable[index][2]/fmax;
				int serverstartindex=(int)(pixels.length*(1-x));
				System.arraycopy(array, 0, smoothed_pixels, (clientid*serverstartindex/(no_of_clients_needed))+(datachunk*size_of_each_chunk),size_of_each_chunk);
				chunks_recd++;
				
				
				
				//the following lines added for facebook hackathon
				Bitmap image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
		    	  
			    for (int i = 0; i < h; i++) 
			    {
			        for (int j = 0; j < w; j++) 
			        {			          
			        	image.setPixel(j, i, smoothed_pixels[i*w+j]);		        		
			        }
			     }
			    
			    //FileOutputStream fout=new FileOutputStream("/storage/sdcard0/received_image.png");
			    if(completelyZero(smoothed_pixels)!=1)
			    {
			    FileOutputStream fout=new FileOutputStream("/storage/sdcard0/received_image.png");
			    image.compress(Bitmap.CompressFormat.PNG, 100, fout);
			    fout.flush();
			    fout.close();
			    }
			    
			    
				
				
				//the *5 part in the next line was added for facebook hackathon
				if(chunks_recd==(no_of_times_client_calls_image_data)*no_of_clients_needed*10)
			    {
	
					//possible infinite loop situation
					while(server_done!=1)
					{
						//System.out.println("Infinite loop!");
					}
					
						image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
			    	  
					    for (int i = 0; i < h; i++) 
					    {
					        for (int j = 0; j < w; j++) 
					        {			          
					        	image.setPixel(j, i, smoothed_pixels[i*w+j]);		        		
					        }
					     }
					    
					    if(completelyZero(smoothed_pixels)!=1)
					    {
					    //FileOutputStream fout=new FileOutputStream("/storage/sdcard0/received_image.png");
					    FileOutputStream fout=new FileOutputStream("/storage/sdcard0/received_image.png");
					    image.compress(Bitmap.CompressFormat.PNG, 100, fout);
					    fout.flush();
					    fout.close();
					    }
					    timeafter=System.currentTimeMillis();
					    
					    //for facebook hackathon
					    
					    
					    System.out.println(" ");
				    	System.out.println("everything done!. Time: "+(timeafter-timebefore)+" ms.");
				    	System.out.println(" ");		    	
				    	timer_flag=0;
			    }
			}			
			catch(Exception e)
			{
			   	e.printStackTrace();
			}
			}
		}
	
		//for facebook hackathon
		public void setImageView(String path)
		{
			 Bitmap bitmap = BitmapFactory.decodeFile(path);
		     ImageView myImageView = (ImageView)findViewById(R.id.imageview);
		     myImageView.setImageBitmap(bitmap);
		}
		
		public int getSizeOfEachChunk() throws BusException
		{
			return size_of_each_chunk;
		}
    }
    
    class ServerBusHandler extends Handler {
       
    	private static final String SERVICE_NAME = "org.alljoyn.bus.samples.simple";
        private static final short CONTACT_PORT=42;
        
        private BusAttachment mBus;
        public static final int CONNECT = 1;
        public static final int DISCONNECT = 2;

        public ServerBusHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            /* Connect to the bus and start our service. */
            case CONNECT: { 
            	//System.out.println("\n\n\n\n\n\n\nService is getting started!\n\n\n\n\n\n");
              
                mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);
                mBus.registerBusListener(new BusListener());
                Status status = mBus.registerBusObject(mSimpleService, "/SimpleService");
                logStatus("BusAttachment.registerBusObject()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
                
                SessionOpts sessionOpts = new SessionOpts();
                sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
                sessionOpts.isMultipoint = false;
                sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
                sessionOpts.transports = SessionOpts.TRANSPORT_ANY;

                status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {
                    @Override
                    public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                        if (sessionPort == CONTACT_PORT) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                logStatus(String.format("BusAttachment.bindSessionPort(%d, %s)",
                          contactPort.value, sessionOpts.toString()), status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
                
                status = mBus.requestName(SERVICE_NAME, flag);
                logStatus(String.format("BusAttachment.requestName(%s, 0x%08x)", SERVICE_NAME, flag), status);
                if (status == Status.OK) {
               	status = mBus.advertiseName(SERVICE_NAME, SessionOpts.TRANSPORT_ANY);
                    logStatus(String.format("BusAttachement.advertiseName(%s)", SERVICE_NAME), status);
                    if (status != Status.OK) {
                       status = mBus.releaseName(SERVICE_NAME);
                        logStatus(String.format("BusAttachment.releaseName(%s)", SERVICE_NAME), status);
                    	finish();
                    	return;
                    }
                }
                
                break;
            }
            
            case DISCONNECT: {
                mBus.unregisterBusObject(mSimpleService);
                mBus.disconnect();
                mClientBusHandler.getLooper().quit();
                break;   
            }

            default:
                break;
            }
        }
    }
    
    private ServerBusHandler mServerBusHandler;
    
    public void switchFunctionality(View view)
    {
    	File f=new File(Environment.getExternalStorageDirectory(),"done_file");
    	try {f.createNewFile();} catch (IOException e) {e.printStackTrace();}
    	System.out.println(" ");
    	System.out.println(" ");
    	System.out.println("Switch!");    	    	
    	System.out.println(" ");
    	System.out.println(" ");
    	mClientBusHandler.sendEmptyMessage(ClientBusHandler.DISCONNECT);
    	
    	/* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
        HandlerThread busThread = new HandlerThread("ServerBusHandler");
        busThread.start();
        mServerBusHandler = new ServerBusHandler(busThread.getLooper());

        /* Start our service. */
        mSimpleService = new SimpleService();
        mServerBusHandler.sendEmptyMessage(ServerBusHandler.CONNECT);
    	
    }
    
    
	/******************************************************server code ends*************************************************************/
    
    
    
    
	private static final int MESSAGE_PING = 1;
	private static final int MESSAGE_PING_REPLY = 2;
	private static final int MESSAGE_POST_TOAST = 3;
	private static final int MESSAGE_START_PROGRESS_DIALOG = 4;
	private static final int MESSAGE_STOP_PROGRESS_DIALOG = 5;

	private static final String TAG = "SimpleClient";

	private EditText mEditText;
	private ArrayAdapter<String> mListViewArrayAdapter;
	private ListView mListView;
	private Menu menu;

	/* Handler used to make calls to AllJoyn methods. See onCreate(). */
	private ClientBusHandler mClientBusHandler;

	private ProgressDialog mDialog;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_PING:
				String ping = (String) msg.obj;
				mListViewArrayAdapter.add("Ping:  " + ping);
				break;
			case MESSAGE_PING_REPLY:
				String ret = (String) msg.obj;
				mListViewArrayAdapter.add("Reply:  " + ret);
				mEditText.setText("");
				break;
			case MESSAGE_POST_TOAST:
				Toast.makeText(getApplicationContext(), (String) msg.obj,
						Toast.LENGTH_LONG).show();
				break;
			case MESSAGE_START_PROGRESS_DIALOG:
				mDialog = ProgressDialog.show(Client.this, "",
						"Finding Simple Service.\nPlease wait...", true, true);
				break;
			case MESSAGE_STOP_PROGRESS_DIALOG:
				mDialog.dismiss();
				break;
			default:
				break;
			}
		}
	};

	private int facebook_m_interval = 50; // 0.05 seconds by default, can be changed later
	
	private Handler facebook_m_handler;
	double starttime;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//facebook
		setImageViewOnCreate("/storage/sdcard0/received_image.png");
		facebook_m_handler = new Handler();
		startRepeatingTask();
		
		//for facebook hackathon, the next line was modified to incorporate participants as the 3rd parameter.
		mListViewArrayAdapter = new ArrayAdapter<String>(this, R.layout.message,participants);
		mListView = (ListView) findViewById(R.id.ListView);
		mListView.setAdapter(mListViewArrayAdapter);
		
		mEditText = (EditText) findViewById(R.id.EditText);
		mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					public boolean onEditorAction(TextView view, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_NULL
								&& event.getAction() == KeyEvent.ACTION_UP) {
							/* Call the remote object's Ping method. */
							Message msg = mClientBusHandler.obtainMessage(
									ClientBusHandler.PING, view.getText().toString());
						
							mClientBusHandler.sendMessage(msg);
						}
						return true;
					}
				});//listener ends here
		HandlerThread busThread = new HandlerThread("ClientBusHandler");
		busThread.start();
		mClientBusHandler = new ClientBusHandler(busThread.getLooper());

		/* Connect to an AllJoyn object. */
		mClientBusHandler.sendEmptyMessage(ClientBusHandler.CONNECT);
		mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
		
	}
	
	//facebook hackathon
	public void setImageViewOnCreate(String path)
	{
		 Bitmap bitmap = BitmapFactory.decodeFile(path);
	     ImageView myImageView = (ImageView)findViewById(R.id.imageview);
	     myImageView.setImageBitmap(bitmap);
	}
	public double seconds=0;
	Runnable facebook_m_statusChecker = new Runnable()
	{
	     public void run() {
	    	  
	    	  setImageViewOnCreate("/storage/sdcard0/received_image.png");
	    	  double curtime=System.currentTimeMillis();
	    	  double time=(curtime-starttime)/1000;
	    	  EditText e=(EditText)findViewById(R.id.EditText);
	    	  if(timer_flag==1)
	    	  e.setText("Time: "+time+" s");
	          facebook_m_handler.postDelayed(facebook_m_statusChecker, facebook_m_interval);
	     }
	};
	void startRepeatingTask()
	{
	    facebook_m_statusChecker.run(); 
	}

	void stopRepeatingTask()
	{
	    facebook_m_handler.removeCallbacks(facebook_m_statusChecker);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		this.menu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.quit:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		/* Disconnect to prevent resource leaks. */
		mClientBusHandler.sendEmptyMessage(ClientBusHandler.DISCONNECT);
	}

	/* This class will handle all AllJoyn calls. See onCreate(). */
	class ClientBusHandler extends Handler {
		private static final String SERVICE_NAME = "org.alljoyn.bus.samples.simple";
		private static final short CONTACT_PORT = 42;

		private BusAttachment mBus;
		private ProxyBusObject mProxyObj;
		private SimpleInterface mSimpleInterface;

		private int mSessionId;
		private boolean mIsInASession;
		private boolean mIsConnected;
		private boolean mIsStoppingDiscovery;

		/* These are the messages sent to the BusHandler from the UI. */
		public static final int CONNECT = 1;
		public static final int JOIN_SESSION = 2;
		public static final int DISCONNECT = 3;
		public static final int PING = 4;
		
		public ClientBusHandler(Looper looper) {
			super(looper);

			mIsInASession = false;
			mIsConnected = false;
			mIsStoppingDiscovery = false;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			/*
			 * Connect to a remote instance of an object implementing the
			 * SimpleInterface.
			 */
			case CONNECT: {
					mBus = new BusAttachment(getPackageName(),BusAttachment.RemoteMessage.Receive);
					mBus.registerBusListener(new BusListener() {
					@Override
					public void foundAdvertisedName(String name,
							short transport, String namePrefix) {
						logInfo(String
								.format("MyBusListener.foundAdvertisedName(%s, 0x%04x, %s)",
										name, transport, namePrefix));
						
						if (!mIsConnected) {
							Message msg = obtainMessage(JOIN_SESSION, name);
							sendMessage(msg);
						}
					}
				});

				Status status = mBus.connect();
				logStatus("BusAttachment.connect()", status);
				if (Status.OK != status) {
					finish();
					return;
				}

				status = mBus.findAdvertisedName(SERVICE_NAME);
				logStatus(String.format(
						"BusAttachement.findAdvertisedName(%s)", SERVICE_NAME),
						status);
				if (Status.OK != status) {
					finish();
					return;
				}

				break;
			}
			case (JOIN_SESSION): {
				if (mIsStoppingDiscovery) {
					break;
				}
				short contactPort = CONTACT_PORT;
				SessionOpts sessionOpts = new SessionOpts();
				Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

				Status status = mBus.joinSession((String) msg.obj, contactPort,
						sessionId, sessionOpts, new SessionListener() {
							@Override
							public void sessionLost(int sessionId) {
								mIsConnected = false;
								logInfo(String.format(
										"MyBusListener.sessionLost(%d)",
										sessionId));
								mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
							}
						});
				logStatus("BusAttachment.joinSession() - sessionId: "
						+ sessionId.value, status);

				if (status == Status.OK) {
					mProxyObj = mBus.getProxyBusObject(SERVICE_NAME,"/SimpleService", sessionId.value,new Class<?>[] { SimpleInterface.class });
					mSimpleInterface = mProxyObj.getInterface(SimpleInterface.class);

					mSessionId = sessionId.value;
					mIsConnected = true;
					mHandler.sendEmptyMessage(MESSAGE_STOP_PROGRESS_DIALOG);
					
					try {
						starttime=System.currentTimeMillis();
						timer_flag=1;
						System.out.println("In Client!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						System.out.println("In Client!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						System.out.println("In Client!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						System.out.println("In Client!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						System.out.println("In Client!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						System.out.println("In Client!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						System.out.println("In Client!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						System.out.println("In Client!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						
						double[] numbers=new double[capacity];
						
						int clientid=mSimpleInterface.getClientID();
						System.out.println("Client ID is "+clientid);
						System.out.println("Client ID is "+clientid);
						System.out.println("Client ID is "+clientid);
						System.out.println("Client ID is "+clientid);
						System.out.println("Client ID is "+clientid);
						System.out.println("Client ID is "+clientid);
						
						if(clientid!=-1)
						{
						double frequency=mSimpleInterface.getFrequencyToRunAt();
						System.out.println("Frequency is "+frequency);
						System.out.println("Frequency is "+frequency);
						System.out.println("Frequency is "+frequency);
						System.out.println("Frequency is "+frequency);
						System.out.println("Frequency is "+frequency);
						System.out.println("Frequency is "+frequency);
						
						//then set scaling governor to userspace and then write the frequency to the file
						
						//next 13 lines commented out for facebook hackathon
						/*String f=(new Integer((int)frequency)).toString();
						String min="echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
						String max="echo 1400000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
						String temp="echo "+f+" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_setspeed";
						Process p;
						p = Runtime.getRuntime().exec("su");
						DataOutputStream os = new DataOutputStream(p.getOutputStream());						
						os.writeBytes("echo userspace > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"+"\n");
						os.writeBytes(max+"\n");
						os.writeBytes(min+"\n");
						os.writeBytes(temp+"\n");
						os.writeBytes("exit\n");
						os.flush();*/
					
						int no=mSimpleInterface.giveImageData();//no specifies how many times the giveActualImageData needs to be called
						int size_of_each_chunk=mSimpleInterface.getSizeOfEachChunk();//get this value from the server
						int[] imageData=new int[no*size_of_each_chunk];
						//for(int e=0;e<12;e++)
						//{
						for(int i=0;i<no;i++)
						{
							System.out.println("hahaha");
							int[] data_from_server=new int[size_of_each_chunk];
							data_from_server=mSimpleInterface.giveActualImageData(clientid, i);
							System.arraycopy(data_from_server,0,imageData,i*size_of_each_chunk,data_from_server.length);
						}
						//}
						
						 //start smoothing
						//for( int e=0;e<12;e++)
						//{
						 for(int k=0;k<50;k++)
						 {
							 System.out.println("client iteration no. "+k+" and freq is "+frequency);
							 for(int i=1;i<imageData.length-1;i++)
							 {
								 //first extract argb values from pixels
								 int a0=Color.alpha(imageData[i-1]);
								 int r0=Color.red(imageData[i-1]);
								 int g0=Color.green(imageData[i-1]);
								 int b0=Color.blue(imageData[i-1]);
								 int a=Color.alpha(imageData[i]);
								 int r=Color.red(imageData[i]);
								 int g=Color.green(imageData[i]);
								 int b=Color.blue(imageData[i]);
								 int a1=Color.alpha(imageData[i+1]);
								 int r1=Color.red(imageData[i+1]);
								 int g1=Color.green(imageData[i+1]);
								 int b1=Color.blue(imageData[i+1]);
								 
								 int afinal=(a0+a+a1)/3;
								 int rfinal=(r0+r+r1)/3;
								 int gfinal=(g0+g+g1)/3;
								 int bfinal=(b0+b+b1)/3;
								 
								 imageData[i]=Color.argb(afinal,rfinal,gfinal,bfinal);						
							 }							 
						    
							 //facebook hackathon
							 //every 10 iterations you can send the smoothed data back to the server
							 if(k%5==0)
							 {
							 for(int i=0;i<no;i++)
							 {
								    int[] localarray=new int[size_of_each_chunk];
									System.arraycopy(imageData, i*size_of_each_chunk, localarray, 0, size_of_each_chunk);
								    mSimpleInterface.takeImageData(localarray,clientid,i);
							 } 
							 }
						 }//this is the loop to repeat smoothing 20 30 40 50 times   
						//}
						//for(int e=0;e<12;e++)
						//{
//						   for(int i=0;i<no;i++)
//						   {
//							    int[] localarray=new int[size_of_each_chunk];
//								System.arraycopy(imageData, i*size_of_each_chunk, localarray, 0, size_of_each_chunk);
//							    mSimpleInterface.takeImageData(localarray,clientid,i);
//						   }
						//}
						 timer_flag=0;
						   File file1=new File(Environment.getExternalStorageDirectory(),"done_file");
						   file1.createNewFile();
						}//end if clientid != -1
					} 
					catch (Exception e) {
						// TODO Auto-generated catch block
						System.out.println("damn an error occurred");
						e.printStackTrace();
					}
					
				}
				
				break;
			}

			
			case DISCONNECT: {
				mIsStoppingDiscovery = true;
				if (mIsConnected) {
					Status status = mBus.leaveSession(mSessionId);
					logStatus("BusAttachment.leaveSession()", status);
				
				}
				mBus.disconnect();
				getLooper().quit();
				
				break;
			}

		
			case PING: {
				try {
					if (mSimpleInterface != null) {
						
						
						sendUiMessage(MESSAGE_PING, msg.obj);
						int[] values={2,4,6,8,10,255,1024,1056,0,9999};
						double[] array=new double[capacity];
                		for(int t=0;t<capacity;t++)
                			array[t]=(int) (Math.random()*10);        	
						String reply=null;
                		for(int i=0;i<no_of_times;i++)
						reply = mSimpleInterface.Ping(array, "client2");
                		sendUiMessage(MESSAGE_PING_REPLY, reply);
						
					}
				} catch (BusException ex) {
					logException("SimpleInterface.Ping()", ex);
				}
				break;
			}
			default:
				break;
			}
		}
		
		/* Helper function to send a message to the UI thread. */
		private void sendUiMessage(int what, Object obj) {
			mHandler.sendMessage(mHandler.obtainMessage(what, obj));
		}
	}

	private void logStatus(String msg, Status status) {
		String log = String.format("%s: %s", msg, status);
		if (status == Status.OK) {
			Log.i(TAG, log);
		} else {
			Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
			mHandler.sendMessage(toastMsg);
			Log.e(TAG, log);
		}
	}

	private void logException(String msg, BusException ex) {
		String log = String.format("%s: %s", msg, ex);
		Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
		mHandler.sendMessage(toastMsg);
		Log.e(TAG, log, ex);
	}

	private void logInfo(String msg) {
		Log.i(TAG, msg);
	}
}
