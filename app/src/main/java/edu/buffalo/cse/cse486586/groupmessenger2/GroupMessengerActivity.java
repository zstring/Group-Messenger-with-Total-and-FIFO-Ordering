package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.PriorityBlockingQueue;

/**
* GroupMessengerActivity is the main Activity for the assignment.
*
* @author stevko
*
*/
public class GroupMessengerActivity extends Activity {

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String MESSAGE = "MESSAGE";
    private static final String SEQUENCE_AGREED = "AGREED";
    private static final String SEQUENCE_PROPOSED = "PROPOSED";
    private static final String ACK = "ACK";
    private static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    private static final boolean[] REMOTE_ACTIVE = {true, true, true, true, true};
    private static final int SERVER_PORT = 10000;
    private static final int TOTAL_AVDS = 5;
    private static boolean crash_avd = false;
    private static int msgCount = -1;
    private static int deliveryCount = -1;
    private static VectorClock deliveryClock = new VectorClock();
    private static String uniqId;
    private static int myPort;
    private static int sendMsgCount = -1;
    private static VectorClock sendClock = new VectorClock();
    private static int index;
    private static final String URI_STRING = "edu.buffalo.cse.cse486586.groupmessenger2.provider";
    private static final int PROP_TIME_OUT = 7000;
    private static final int AGREE_TIME_OUT = 11000;
    private static final int TTIME_OUT = 3000;
    ServerTask serverTask;
    private Uri mUri;
    private GroupMessengerProvider gmProvider;
    private static long time_taken=0;

//    private static HashMap<String, Message> bufferQueue = new HashMap<String, Message>();
    private static Hashtable<String, Message> bufferQueue = new Hashtable<String, Message>();
//    private static PriorityQueue<Sequencer> pqSequencer;


//    private static PriorityQueue<Message> pqMessage;
//    private static HashMap<String, Sequencer> sequences = new HashMap<String, Sequencer>();
//    private static HashMap<String, ArrayList<Double>> proposedNo = new HashMap<String, ArrayList<Double>>();
//    private static HashMap<Integer, PriorityQueue<Message>> pqMessage = new HashMap<>();
//    private static HashMap<String, Timer> hmTimerProposal = new HashMap<>();
//    private static HashMap<String, Timer> hmTimerMessage = new HashMap<>();

    private static PriorityBlockingQueue<Sequencer> pqSequencer;
    private static Hashtable<String, Sequencer> sequences = new Hashtable<String, Sequencer>();
    private static Hashtable<String, List<Double>> proposedNo = new Hashtable<String, List<Double>>();
    private static Hashtable<Integer, PriorityBlockingQueue<Message>> pqMessage = new Hashtable<Integer, PriorityBlockingQueue<Message>>();
    private static Hashtable<String, Timer> hmTimerProposal = new Hashtable<String, Timer>();
    private static Hashtable<String, Timer> hmTimerMessage = new Hashtable<String, Timer>();


    private static class SequencerComparator implements Comparator<Sequencer> {
        public int compare(Sequencer a, Sequencer b) {
            if (a.sequenceId < b.sequenceId) {
                return -1;
            } else if (a.sequenceId > b.sequenceId) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private static class Sequencer implements Comparable<Sequencer> {
        private double sequenceId;
        private String msgId;
        private boolean agreed;

        public Sequencer(double sId, String mId) {
            this.sequenceId = sId;
            this.msgId = mId;
            this.agreed = false;
        }

        @Override
        public int compareTo(Sequencer that) {
            if (this.sequenceId < that.sequenceId) {
                return -1;
            } else if (this.sequenceId > that.sequenceId) {
                return 1;
            } else {
                return 0;
            }
        }

        public double getSequenceId() {
            return this.sequenceId;
        }

        public void setSequenceId(double sId) {
            this.sequenceId = sId;
        }

        public String getMsgId() {
            return this.msgId;
        }

        public boolean isAgreed() { return  this.agreed; }

        public void setAgreed() {this.agreed = true; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        mUri = Uri.fromParts("content", URI_STRING, "");
        gmProvider = new GroupMessengerProvider();
        SequencerComparator comp = new SequencerComparator();
//        pqSequencer = new PriorityQueue<Sequencer>(10, comp);
        pqSequencer = new PriorityBlockingQueue<Sequencer>(10, comp);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        uniqId = String.valueOf((Integer.parseInt(portStr) * 2));
        myPort = Integer.parseInt(uniqId);
        for (int i = 0; i < REMOTE_PORTS.length; i++) {
            if (REMOTE_PORTS[i].equals(uniqId)) {
                index = i;
                break;
            }
        }
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            serverTask = new ServerTask();
            serverTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException ex) {
            Log.e("Error: ", ex.getMessage());
        }
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String msg = editText.getText().toString();
                        editText.setText("");
                        String[] ar = {MESSAGE, msg};
                        Message msgObject = new Message(MESSAGE, "", 0.0, msg);
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgObject);
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while (true) {
                try {
                    Socket clientS = serverSocket.accept();
                    Date dStart = new Date();
                    clientS.setSoTimeout(TTIME_OUT);
                    int clientPort = clientS.getPort();
                    InputStream is = clientS.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
//                    InputStreamReader is = new InputStreamReader(clientS.getInputStream());
//                    BufferedReader br = new BufferedReader(is);
                    String msgId, msg;
                    Message receivedMsg = (Message) ois.readObject();
                    String temp = receivedMsg.type;
                    Date dEnd = new Date();
                    time_taken += ((dEnd.getTime() - dStart.getTime()) / 1000);
                    String[] strings1 = {"Server time after converting deser " + time_taken, "F"};
                    publishProgress(strings1);
                    if (temp.equals(SEQUENCE_AGREED)) {
                        //Sender has sent the final Agreed MSG...

                        msgId = receivedMsg.msgId;
                        ///#########################################
                        synchronized (this) {
                            Timer th = hmTimerMessage.get(msgId);
                            if (th == null) {
                                String[] strings = {"No Timer Event exists after gting agreec msg " + msgId, "F"};
                                publishProgress(strings);
                            } else {
                                th.cancel();
                                th.purge();
                            }

                            String[] strings = {"Cancel Timer after rcving agreed msg " + msgId, "F"};
                            publishProgress(strings);

                            hmTimerMessage.remove(msgId);
                            ///#########################################
                            double sequencerId = receivedMsg.sequenceId;
                            Sequencer sq = sequences.get(msgId);
                            if (sq == null) {
                                String[] strs = {"No sequences obj exist may be delted before getting agreed msg " + msgId, "F"};
                                publishProgress(strs);
                            } else if (sq != null) {
                                pqSequencer.remove(sq);
                                sq.setSequenceId(sequencerId);
                                pqSequencer.add(sq);
                                sq.setAgreed();
                            }
                        }
                        checkAndDeliverMsg();
                    }
                    else if (temp.equals(SEQUENCE_PROPOSED)) {
                        //This is for getting proposed msgs

                        msgId = receivedMsg.msgId;
                        synchronized (this) {
                            double propSId = receivedMsg.sequenceId;
                            List<Double> ids;
                            if (proposedNo.containsKey(msgId)) {
                                ids = proposedNo.get(msgId);
                                ids.add(propSId);
                                //If all has proposed then agreed on maximum number
                                // false for no failure normal agreement checking
                                checkAndSendAgreement(msgId, false);
                            } else {
                                ids = Collections.synchronizedList(new ArrayList<Double>());

                                ids.add(propSId);
                                proposedNo.put(msgId, ids);
                            }
                            String[] strs = {"Got the proposal for msg id " + msgId + " total proposal= " + ids.size(), "F"};
                            publishProgress(strs);
                        }

                        if (!hmTimerProposal.containsKey(msgId)) {
                            Timer tProposal = new Timer();
                            TimerHelper th = new TimerHelper(SEQUENCE_PROPOSED, msgId);

                            tProposal.schedule(th, PROP_TIME_OUT);
//                            tProposal.schedule(th, PROP_TIME_OUT);
                            hmTimerProposal.put(msgId, tProposal);
                            String[] ss = {"Creatng proposal timer for msgId: " + msgId, "F"};
                            publishProgress(ss);
                        }
                    }
                    else {
                        msgId = receivedMsg.msgId;
                        msg = receivedMsg.msg;
                        synchronized (this) {
                            bufferQueue.put(msgId, receivedMsg);
//                        String [] msgToSend = {SEQUENCE_PROPOSED, 11108 + "", ++msgCount+"", msg};

                            //We have to propose a sequence no of this message
                            msgCount++;

                            double nextSequence = msgCount + 0.1 * (index + 1);
                            Sequencer newS = new Sequencer(nextSequence, msgId);
                            sequences.put(msgId, newS);
                            Message msgToSend = new Message(SEQUENCE_PROPOSED, msgId, nextSequence, "");
                            msgToSend.setRemotePort(receivedMsg.remotePort);
                            pqSequencer.add(newS);
//                        new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msgToSend);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);

                            //Adding Timer Event to receive Msg
                            //If within sometime boudn we do not get the msg then ignore/delete the msg.
                            // Sender has crashed and not able to send the agreement

                            Timer tMessage = new Timer();
                            TimerHelper th = new TimerHelper(MESSAGE, msgId);
                            tMessage.schedule(th, AGREE_TIME_OUT);
                            hmTimerMessage.put(msgId, tMessage);
                        }
                    }

                    clientS.close();

                } catch (SocketTimeoutException ex) {
                    //Updating the Client as Failed Node
                    Log.e("Error: ", ex.getMessage() + " Server Catch Exception");
//                    REMOTE_ACTIVE[0] = false;
                } catch (IOException ex) {
//                    REMOTE_ACTIVE[0] = false;
                    Log.v("Error: ", ex.getMessage()  + "  Server Catch Exception");
                } catch (ClassNotFoundException ex) {
                    Log.e("Class Not Found Exceptop ", ex.getMessage() + " Server Catch Exception");
                }
            }
//            return null;
        }

        private void checkAndSendAgreement(String msgId, boolean failure) {

            int maxSize = 0;
            synchronized (this) {
                if (failure) {
                    crash_avd = true;
                }
            }

            List<Double> ids = proposedNo.get(msgId);
            //ids may be null as it already removed from the
            //map when
            if (ids == null) return;
            for (int i = 0; i < REMOTE_ACTIVE.length; i++) {
                if (REMOTE_ACTIVE[i]) maxSize++;
            }
            if (failure) {
                Date d = new Date();
                String[] strings = {"AVD failure detected due to proposal time out " + msgId + " " + d.getTime(), "F"};
                publishProgress(strings);
            }
//            maxSize = maxSize - nodes;
//            if (maxSize == REMOTE_ACTIVE.length)
//                maxSize = maxSize + nodes;
            if (crash_avd && maxSize == REMOTE_ACTIVE.length) {
                maxSize--;
            }
            String[] stringss = {"count for proposal msgid " + msgId + " Flag for crash avd " + crash_avd + " Size " + maxSize + " ids.isze " + ids.size(), "F"};
            publishProgress(stringss);

            if (ids.size()  >= maxSize) {
                double maxAgreedId = 0.0;
                for (double d : ids) {
                    if (maxAgreedId < d) {
                        maxAgreedId = d;
                    }
                }
                //We have to multicast the final agreed Ids;
                String[] msgToSend = {SEQUENCE_AGREED, msgId, maxAgreedId+""};
                Message newAgreed = new Message(SEQUENCE_AGREED, msgId, maxAgreedId, "");
                proposedNo.remove(msgId);
                Timer tProposal = hmTimerProposal.get(msgId);

//                hmTimerProposal.remove(msgId);
                if (!failure) {
                    tProposal.cancel();
                    tProposal.purge();
                    String[] strings = {"Cancelling PROPOSAL TIMER " + msgId, "F"};
                    publishProgress(strings);
                }

//                new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, newAgreed);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, newAgreed);
            }
        }

        private class TimerHelper extends TimerTask {
            private boolean flag;
            private String type;
            private String msgId;
            private Date startTime;

            public TimerHelper(String type, String msgId) {
                this.flag = true;
                this.type = type;
                this.msgId = msgId;
                this.startTime = new Date();
            }
            @Override
            public void run() {

                String[] strings = new String[2];
                Date d = new Date();
                Log.e("Error Failed!!!!!!+++++++++++++++++++++++++++++++++++++++++++++", "");
                if (type.equals(SEQUENCE_PROPOSED)) {
                    Message msgToRemove = bufferQueue.get(msgId);
                    //Checking whether sender avd is up or not
                        checkAndSendAgreement(msgId, true);
                        strings[0] = " All avd didn't propose on time, FAILED::" + msgId + ",, " + (d.getTime() - startTime.getTime()) + " Time " + d.getTime();
                } else {
                    //removeAndDeliver(msgId);
                    Message msgToRemove = bufferQueue.get(msgId);

                    bufferQueue.remove(msgId);
                    Sequencer sqObj = sequences.get(msgId);
                    pqSequencer.remove(sqObj);
                    checkAndDeliverMsg();
                    strings[0] = "Avd didn't send agreement on time, FAILED::" + msgId + ",, " + (d.getTime() - startTime.getTime()) + " Time " + d.getTime();
                }
                strings[1] = "F";
                publishProgress(strings);
            }

            public void setFlag() {
                flag = false;
            }

            public boolean getFlag() {
                return flag;
            }
        }

        private void checkAndDeliverMsg() {
                while (!pqSequencer.isEmpty()) {
                    Sequencer minSeq = pqSequencer.peek();

                    if (minSeq.isAgreed()) {
                        String sMsgId = minSeq.getMsgId();
                        pqSequencer.poll();
                        checkFIFOAndDeliverMsg(sMsgId, minSeq.getSequenceId());
                    } else {
                        publishProgress(new String[]{"Topper is not agreed yet, " + minSeq.msgId, "F"});
                        break;
                    }
                }
        }

        private void checkFIFOAndDeliverMsg(String msgId, double priorityId) {
            Message msgObj = bufferQueue.get(msgId);
//            if (msgObj == null) return;
            bufferQueue.remove(msgId);
            if (msgObj == null) {
                //Dont know why null
                Log.e("REMOTE PORT", "Msg id not present somehow to deliver " + msgId);
                return;
            }

            String remotePortMsg = msgObj.remotePort + "";

            int i = 0;
            for (; i < REMOTE_PORTS.length; i++) {
                if (REMOTE_PORTS[i].equals(remotePortMsg)) {
                    break;
                }
            }
//            PriorityQueue<Message> pqM;
            PriorityBlockingQueue<Message> pqM;
            if(pqMessage.containsKey(i)) {
                pqM = pqMessage.get(i);
                pqM.add(msgObj);
            }
            else {
//                pqM = new PriorityQueue<>();
                pqM = new PriorityBlockingQueue<Message>();
                pqM.add(msgObj);
                pqMessage.put(i, pqM);
            }
            while (!pqM.isEmpty()) {
                Message topMessage = pqM.peek();
                VectorClock vcMsg = topMessage.vectorClock;
                if (deliveryClock.clock[i] + 1 == vcMsg.clock[i]) {
                    deliveryClock.clock[i]++;
                    String[] sSend = {msgObj.msg, msgObj.sequenceId + " msgId: " + msgObj.msgId + " Priority id: "+ priorityId};
                    publishProgress(sSend);
                    pqM.remove(topMessage);
                }
                else {
                    break;
                }
            }
        }

        protected void onProgressUpdate(String... strings) {

            String msg = strings[0].trim();
            String order = strings[1].trim();

            if (!order.equals("F"))
                deliveryCount++;
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(deliveryCount + "\t" + order + "\t" + msg + "\n");
            if (!order.equals("F")) {
                ContentValues cv = new ContentValues();
                cv.put(KEY_FIELD, deliveryCount + "");
                cv.put(VALUE_FIELD, msg);
                gmProvider.insert(mUri, cv);
            }
//            publishProgress(msg);
        }
    }

    private class ClientTask extends AsyncTask<Message, String, Boolean> {

        @Override
        protected Boolean doInBackground(Message... params) {
            Message msgObject = params[0];
            String parameter = msgObject.type;
            if (parameter.equals(SEQUENCE_PROPOSED)) {
                int remotePort = msgObject.remotePort;//params[1];
                String remotePortString = remotePort +"";
                double sId = msgObject.sequenceId;
                String msgId = msgObject.msgId;//params[3];
                int remoteClient = -1;

                for (int i = 0; i < REMOTE_PORTS.length; i++) {
                    if (REMOTE_PORTS[i].equals(remotePortString)) {
                        remoteClient = i;
                        break;
                    }
                }
//                TimerHelper th = new TimerHelper();
                try {
                    Date dStart = new Date();
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            remotePort);
//                    socket.setSoTimeout(TTIME_OUT-2500);
//                    Timer timertoAcceptAgreement = new Timer();
//                    timertoAcceptAgreement.schedule(th, TIME_OUT);

                    // Start the timer that we should recieve the agreement msg in
                    // within soem time bound
                    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    Message msgObjectSend = new Message(SEQUENCE_PROPOSED, msgId, sId, "");
                    oos.writeObject(msgObjectSend);
                    oos.flush();
                    oos.close();
//                    timertoAcceptAgreement.cancel();
                    socket.close();
                    String[] str = {" Propose no for msg id " + msgId + " ", "F"};
                    publishProgress(str);
                    Date dEnd = new Date();
                    time_taken += ((dEnd.getTime() - dStart.getTime()) / 1000);

                } catch (UnknownHostException ex) {
//                    th.setFlag();
                    if (remoteClient != -1)
                        REMOTE_ACTIVE[remoteClient] = false;
                    Log.e("Error: Server failed to accept Proposal " + remoteClient + ", ", ex.getMessage());
                } catch (IOException ex){
//                    th.setFlag();
                    if (remoteClient != -1)
                        REMOTE_ACTIVE[remoteClient] = false;
                    Log.e("Error: Server failed to accept Proposal " + remoteClient + ", ", ex.getMessage());
                }
//                finally {
//                    if (!th.getFlag()) {
//                        REMOTE_ACTIVE[remoteClient] = false;
//                        Log.e("Error: Server failed to accept Proposal ", remoteClient +" ");
//                        bufferQueue.remove(msgId);
//                        Sequencer sqObj = sequences.get(msgId);
//                        pqSequencer.remove(sqObj);
//                        sequences.remove(msgId);
//                    }
//                }

            }
            else {
                if (parameter.equals(MESSAGE)) {
                    sendMsgCount++;
                    sendClock.updateClock(index);
                }
                Date dStart = new Date();
                for (int i = 0; i < TOTAL_AVDS; i++) {
                    if (REMOTE_ACTIVE[i]) {
//                        String[] str = {" REMOTE ACTIVE " + i + " " + REMOTE_PORTS[i], "F"};
//                        publishProgress(str);
                        try {
                            String remotePort = REMOTE_PORTS[i];
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));
//                            socket.setSoTimeout(TIME_OUT);
//                            socket.setSoTimeout(TTIME_OUT-2500);
                            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                            //A dummy Unique Id
                            if (parameter.equals(MESSAGE)) {
                                String msgToSend = msgObject.msg;
                                String msgId = uniqId + "" + sendMsgCount;
                                //We shud get the proposal for the msg in some time bound..

                                Message msgObjectSend = new Message(MESSAGE, msgId, 0.0, msgToSend);
                                msgObjectSend.setVectorClock(sendClock);
                                msgObjectSend.setRemotePort(Integer.parseInt(uniqId));
                                oos.writeObject(msgObjectSend);
                            }
                            else if (parameter.equals(SEQUENCE_AGREED)) {
                                String msgId = msgObject.msgId;
                                double sId = msgObject.sequenceId;
                                Message msgObjectSend = new Message(SEQUENCE_AGREED, msgId, sId, "");
                                oos.writeObject(msgObjectSend);
                            }
                            oos.flush();
                            oos.close();
                            socket.close();
                        } catch (SocketTimeoutException ex) {
                            REMOTE_ACTIVE[i] = false;
                            Log.e("Error: AVD FAILED " + i + ", ", ex.getMessage());
                        } catch (UnknownHostException ex) {
                            REMOTE_ACTIVE[i] = false;
                            Log.e("Error: AVD FAILED " + i + ", ", ex.getMessage());
                        } catch (IOException ex) {
                            REMOTE_ACTIVE[i] = false;
                            Log.e("Error: AVD FAILED " + i + ", ", ex.getMessage());
                        }
                    }
                }
                Date dEnd = new Date();
                time_taken += ((dEnd.getTime() - dStart.getTime()) / 1000);
            }

            String[] str = {" Client Task Time taken till now in sending " + time_taken + " ", "F"};
            publishProgress(str);
            return true;
        }


        protected void onProgressUpdate(String... strings) {

            String msg = strings[0].trim();
            String order = strings[1].trim();

            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(deliveryCount + "\t" + order + "\t" + msg + "\n");
        }
    }


    private static class VectorClock implements Serializable, Comparable<VectorClock> {
        int[] clock;
        public VectorClock() {
            clock = new int[TOTAL_AVDS];
            for (int i =0; i < clock.length; i++) {
                clock[i] = 0;
            }
        }

        public VectorClock(VectorClock v) {
            clock = new int[TOTAL_AVDS];
            for (int i = 0; i < v.clock.length; i++) {
                clock[i] = v.clock[i];
            }
        }

        public void updateClock(int i) {
            clock[i] = clock[i] + 1;
        }

        public int compareTo(VectorClock that) {
            for (int i = 0; i < clock.length; i++) {
                if (this.clock[i] < that.clock[i]) {
                    return -1;
                }
                else if (this.clock[i] > that.clock[i]) {
                    return 1;
                }
            }
            return 0;
        }
    }

    private static class Message implements Serializable, Comparable<Message> {
        private static final long serialVersionUID = -68497944767710L;
        public String type;
        public String msgId;
        public Double sequenceId;
        public String msg;
        public int remotePort;
        public VectorClock vectorClock;
        public Message(String t, String msgId, double sId, String msg) {
            this.type = t;
            this.msgId = msgId;
            this.sequenceId = sId;
            this.msg = msg;

        }

        public void setVectorClock(VectorClock vc) {
            vectorClock = vc;
        }

        public void setRemotePort(int port){
            this.remotePort = port;
        }

        public int compareTo(Message that) {
            return this.vectorClock.compareTo(that.vectorClock);
        }
    }
}
