package dashboard.iot.bku;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;




public class MainActivity extends AppCompatActivity {

    MQTTHelper mqttHelper;
    TextView txtTemp, txtHummi;
    ToggleButton toggleButton;

    // creating a variable
    // for our graph view.
    GraphView graphView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        toggleButton = findViewById(R.id.toggleButton1);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
                toggleButton.setVisibility(View.INVISIBLE);
                if(isChecked == true) {
                    Log.d("mqtt", "Button is  checked");
                    sendDataMQTT("izayazuna/feeds/button", "1");
                }
                else
                {
                    Log.d("mqtt:", "Button is  not checked");
                    sendDataMQTT("izayazuna/feeds/button", "0");
                }
            }
        });

        txtTemp = findViewById(R.id.txtTemperature);
        txtHummi = findViewById(R.id.txtHumidity);
        txtTemp.setText("40" + "*C");
        txtHummi.setText("80" + "%");
        startMQTT();
        setupScheduler();




        // on below line we are initializing our graph view.
        graphView = findViewById(R.id.idGraphViewTemp);
        // on below line we are adding data to our graph view.
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[]{
                // on below line we are adding
                // each point on our x and y axis.
                new DataPoint(0, 16),
                new DataPoint(1, 30),
                new DataPoint(2, 40),
                new DataPoint(3, 19),
                new DataPoint(4, 16),
                new DataPoint(5, 31),
                new DataPoint(6, 16),
                new DataPoint(7, 21),
                new DataPoint(8, 34)
        });
        // after adding data to our line graph series.
        // on below line we are setting
        // title for our graph view.
        graphView.setTitle("My Graph Temperature View");

        // on below line we are setting
        // text color to our graph view.
        graphView.setTitleColor(R.color.colorAccent);

        // on below line we are setting
        // our title text size.
        graphView.setTitleTextSize(18);

        // on below line we are adding
        // data series to our graph view.
        graphView.addSeries(series);


        //HUMIDITY
        // on below line we are initializing our graph view.
        graphView = findViewById(R.id.idGraphViewHumi);
        // on below line we are adding data to our graph view.
        LineGraphSeries<DataPoint> series_next = new LineGraphSeries<DataPoint>(new DataPoint[]{
                // on below line we are adding
                // each point on our x and y axis.
                new DataPoint(0, 20),
                new DataPoint(1, 30),
                new DataPoint(2, 70),
                new DataPoint(3, 50),
                new DataPoint(4, 60),
                new DataPoint(5, 90),
                new DataPoint(6, 60),
                new DataPoint(7, 40),
                new DataPoint(8, 80)
        });
        // after adding data to our line graph series.
        // on below line we are setting
        // title for our graph view.
        graphView.setTitle("My Graph Humidity View");

        // on below line we are setting
        // text color to our graph view.
        graphView.setTitleColor(R.color.colorPrimaryDark);

        // on below line we are setting
        // our title text size.
        graphView.setTitleTextSize(18);

        // on below line we are adding
        // data series to our graph view.
        graphView.addSeries(series_next);

    }
    @Override
            protected void onPause()
    {
        super.onPause();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    int waiting_period = 3;
    boolean send_message_agian = false;
    int resend_counter = 3;
    List<MQTTMessage> list = new ArrayList<MQTTMessage>();



    private void setupScheduler()
    {
        Timer aTimer = new Timer();
        TimerTask scheduler = new TimerTask() {
            @Override
            public void run() {
                if(waiting_period > 0)
                {
                    waiting_period--;
                    if(waiting_period == 0)
                    {
                        send_message_agian = true;
                        resend_counter--;
                        if(resend_counter == 0)
                        {
                            waiting_period = 0;
                            send_message_agian = false;
                            resend_counter = 3;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    toggleButton.setVisibility(View.VISIBLE);
                                }
                            });
                            list.clear();
                            Log.d("mqtt", "size" + list.size() + " and delete message after 3 times resent");
                        }
                    }
                    if(send_message_agian)
                    {
                        Log.d("mqtt", "Resent again data form " + list.get(0).topic.toString());
                        sendDataMQTT(list.get(0).topic, list.get(0).mess);
                        list.remove(0);
                    }
                }
            }
        };
        aTimer.schedule(scheduler, 0, 1000);
    }



    private void sendDataMQTT(String topic, String value)
    {
        waiting_period = 3;
        send_message_agian = false;
        MQTTMessage aMessage = new MQTTMessage();
        aMessage.topic = topic;
        aMessage.mess = value;
        list.add(aMessage);





        MqttMessage msg = new MqttMessage();
        //Set cac thong so
        msg.setId(1234);//id cua message
        msg.setQos(0);//Set quaility of service(0-4), cang cao thi du lieu cang tin cay
        msg.setRetained(true);


        byte[] b = value.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);

        }catch (MqttException e){
        }
    }
    private void startMQTT()
    {
        mqttHelper = new MQTTHelper(getApplicationContext(), "123456");
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d("mqtt", "Connection is successful" );
            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("mqtt", "Recieved " + message.toString() + " from topic " + topic.toString());
                if(topic.equals("izayazuna/feeds/temperature"))
                {
                    String t = String.format("%s °C",message.toString());
                    txtTemp.setText(t);
                }
                if(topic.equals("izayazuna/feeds/hummidity"))
                {
                    String t = String.format("%s %%",message.toString());
                    txtHummi.setText(t);
                }
                if(topic.equals("izayazuna/feeds/error"))
                {
                    waiting_period = 0;
                    send_message_agian = false;
                    resend_counter = 3;
                    toggleButton.setVisibility(View.VISIBLE);
                }
                if(topic.equals("izayazuna/feeds/button"))
                {
                    int btn = Integer.parseInt(message.toString());
                    if(btn == 0)
                    {
                        toggleButton.setChecked(false);
                    }
                    else
                    {
                        toggleButton.setChecked(true);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
    public class MQTTMessage{
        public String topic;
        public String mess;
    }


}
