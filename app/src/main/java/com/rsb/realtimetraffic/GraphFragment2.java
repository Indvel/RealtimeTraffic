package com.rsb.realtimetraffic;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class GraphFragment2 extends Fragment {

    String apiKey = "9731594721";
    String[] routes = {"경부선","남해선","서해안선","중부선","논산천안선","영동선","수도권제1순환선","제2경인선","경인선"};
    String[] timeBox = {"양양", "강릉", "대전", "대구", "광주", "울산", "부산", "목포"};
    JSONObject json;
    LineChart lineChart, lineChart2;
    ArrayList<Entry> entries = new ArrayList<>();
    ArrayList<Entry> entries2 = new ArrayList<>();

    ArrayList<Entry> entriesTime = new ArrayList<>();
    ArrayList<Integer> timeList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.graph_fragment2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lineChart = view.findViewById(R.id.lineChart);
        lineChart.setTouchEnabled(false);

        lineChart2 = view.findViewById(R.id.lineChart2);
        lineChart2.setTouchEnabled(false);

        timerTask();

        getTakeTimeData();
    }

    public void timerTask() {
        Timer t = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                getHighwayData();
                getTakeTimeData();
            }
        };
        t.schedule(task, 0, 10000); //10초에 한 번 실행
    }

    public void getHighwayData() {
        new ThreadTask<String, String>() {
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected String doInBackground(String arg) {

                try {
                    Document doc = Jsoup.connect("https://data.ex.co.kr/openapi/odtraffic/trafficAmountByCongest?key=" + apiKey + "&type=xml&tmType=3")
                            .timeout(3000)
                            .ignoreContentType(true)
                            .get(); //API 호출(한국도로공사)

                    Elements data = doc.select("list"); //결과값 목록 불러오기
                    json = new JSONObject(); //JSON 생성

                    for(Element e : data) { //목록을 한 개씩 가져옴
                        String split= "";
                        //고속도로명 분류하기(남해선, 경부선 등)
                        split = e.select("routeName").text();
                        if(split.contains("(")) {
                            split = split.substring(0, split.indexOf("(")).trim();
                        }
                        if(!json.has(split)) {
                            JSONObject obj = new JSONObject();
                            if(Arrays.asList(routes).contains(split)) {
                                obj.put("ratioSum", Integer.valueOf(e.select("shareRatio").text()));
                                obj.put("speedSum", Integer.valueOf(e.select("speed").text()));
                                obj.put("allCount", 1);
                                json.put(split, obj);
                            }
                        } else {
                            JSONObject obj = new JSONObject();
                            int val = json.getJSONObject(split).getInt("ratioSum") + Integer.valueOf(e.select("shareRatio").text());
                            obj.put("ratioSum", val);
                            int val2 = json.getJSONObject(split).getInt("speedSum") + Integer.valueOf(e.select("speed").text());
                            obj.put("speedSum", val2);
                            if(json.getJSONObject(split).has("allCount")) {
                                obj.put("allCount", json.getJSONObject(split).getInt("allCount") + 1);
                            } else {
                                obj.put("allCount", 1);
                            }
                            json.put(split, obj);
                        }
                    }

                } catch(IOException | JSONException e) {
                    e.printStackTrace();
                }

                return "";
            }

            @Override
            protected void onPostExecute(String result) {
                if(entries.isEmpty() || entries2.isEmpty()) {
                    drawLineGraph1();
                } else {
                    updateLineGraph1();
                }
            }
        }.execute("");
    }

    public void drawLineGraph1() {
        try{
            LineData lineData = new LineData();

            for(int i = 0; i < routes.length; i++) {
                if(json.has(routes[i])) {
                    if(json.getJSONObject(routes[i]).getInt("allCount") > 1) {
                        float value =  (float)json.getJSONObject(routes[i]).getInt("ratioSum") / json.getJSONObject(routes[i]).getInt("allCount");
                        entries.add(new Entry(i, value));
                    } else {
                        int value = json.getJSONObject(routes[i]).getInt("ratioSum");
                        entries.add(new Entry(i, value));
                    }
                } else {
                    entries.add(new Entry(i, 0));
                }
            }
            LineDataSet dataSet = new LineDataSet(entries, "점유율(%)");
            dataSet.setColor(Color.parseColor("#00D8FF"));
            dataSet.setValueTextSize(11);
            dataSet.setValueTextColor(Color.parseColor("#008299"));
            dataSet.setDrawFilled(true);
            if(Utils.getSDKInt() >= 18) {
                dataSet.setFillDrawable(new ColorDrawable(Color.argb(20, 0, 216, 255)));
            } else {
                dataSet.setFillColor(Color.CYAN);
            }

            lineData.addDataSet(dataSet);

            entries2 = new ArrayList<>();
            for(int i = 0; i < routes.length; i++) {
                if(json.has(routes[i])) {
                    if(json.getJSONObject(routes[i]).getInt("allCount") > 1) {
                        float value =  (float)json.getJSONObject(routes[i]).getInt("speedSum") / json.getJSONObject(routes[i]).getInt("allCount");
                        entries2.add(new Entry(i, value));
                    } else {
                        int value = json.getJSONObject(routes[i]).getInt("speedSum");
                        entries2.add(new Entry(i, value));
                    }
                } else {
                    entries2.add(new Entry(i, 0));
                }
            }
            //원활(80km/h), 서행(40~80km/h), 정체(40km/h)
            LineDataSet dataSet2 = new LineDataSet(entries2, "정체 속도(km/h)");
            dataSet2.setColor(Color.parseColor("#2FED28"));
            dataSet2.setCircleColor(Color.WHITE);
            dataSet2.setCircleColor(Color.parseColor("#2FED28"));
            dataSet2.setValueTextSize(11);
            dataSet2.setValueTextColor(Color.parseColor("#2F9D27"));
            dataSet2.setDrawFilled(true);
            if(Utils.getSDKInt() >= 18) {
                dataSet2.setFillDrawable(new ColorDrawable(Color.argb(20, 47, 237, 40)));
            } else {
                dataSet2.setFillColor(Color.CYAN);
            }

            lineData.addDataSet(dataSet2);

            lineChart.clear();
            lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(routes)); //라벨 설정
            lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            lineChart.getXAxis().setLabelRotationAngle(-30);
            lineChart.getXAxis().setTextSize(8);
            lineChart.getXAxis().setDrawAxisLine(true);
            lineChart.animateY(500); //애니메이션(0.5초)

            lineChart.getXAxis().setLabelCount(routes.length, true);
            lineChart.getAxisRight().setDrawLabels(false); //오른쪽 축 숨기기
            lineChart.getAxisRight().setEnabled(false);
            lineChart.getAxisLeft().setTextSize(8);
            lineChart.getAxisLeft().setAxisMinimum(0f);
            lineChart.getAxisLeft().setAxisMaximum(60f);
            lineChart.getDescription().setEnabled(false); //Description 숨기기
            lineChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            lineChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            lineChart.setData(lineData); //차트 데이터 설정
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateLineGraph1() {
        try {
            int changed = 0;
            if (!entries.isEmpty() || !entries2.isEmpty()) {
                for (int i = 0; i < routes.length; i++) {
                    if(json.has(routes[i])) {
                        if (json.getJSONObject(routes[i]).getInt("allCount") > 1) {
                            float value = (float) json.getJSONObject(routes[i]).getInt("ratioSum") / json.getJSONObject(routes[i]).getInt("allCount");
                            if (entries.get(i).getY() != value) {
                                entries.set(i, new Entry(i, value));
                                changed++;
                            }
                            float value2 = (float) json.getJSONObject(routes[i]).getInt("speedSum") / json.getJSONObject(routes[i]).getInt("allCount");
                            if (entries2.get(i).getY() != value2) {
                                entries2.set(i, new Entry(i, value2));
                                changed++;
                            }
                        } else {
                            int value = json.getJSONObject(routes[i]).getInt("ratioSum");
                            if (entries.get(i).getY() != value) {
                                entries.set(i, new Entry(i, value));
                                changed++;
                            }
                            int value2 = json.getJSONObject(routes[i]).getInt("speedSum");
                            if (entries2.get(i).getY() != value2) {
                                entries2.set(i, new Entry(i, value2));
                                changed++;
                            }
                        }
                    }
                }
                lineChart.notifyDataSetChanged();
                lineChart.invalidate();
                if(changed > 0) {
                    lineChart.animateY(500);
                }
            }
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public void getTakeTimeData() {
        new ThreadTask<String, String>() {
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected String doInBackground(String arg) {

                try {
                    Document doc = Jsoup.connect("http://m.roadplus.co.kr/forecast/predict/selectPredictView.do")
                            .timeout(3000)
                            .get();

                    timeList = new ArrayList<>();

                    Elements timeBox = doc.select("#searchForm > div.map_wrap > div.time_box");
                    //남양주-양양, 서울-강릉, 서울-대전, 서울-대구, 서울-울산, 서울-광주, 서울-부산, 서서울-목포
                    for(Element e : timeBox) {
                        String text = e.select("dd.blue").text();
                        int t = (Integer.valueOf(text.split(":")[0]) * 60) + Integer.valueOf(text.split(":")[1]);
                        timeList.add(t);
                    }

                } catch(IOException e) {
                    e.printStackTrace();
                }

                return "";
            }

            @Override
            protected void onPostExecute(String result) {
                if(entriesTime.isEmpty()) {
                    drawLineGraph2();
                } else {
                    updateLineGraph2();
                }
            }
        }.execute("");
    }

    public void drawLineGraph2() {
        try {
            entriesTime = new ArrayList<>();
            for(int i = 0; i < timeList.size(); i++) {
                entriesTime.add(new Entry(i, timeList.get(i)));
            }
            LineDataSet dataSet = new LineDataSet(entriesTime, "도시 간 소요시간(서울-주요도시)");
            dataSet.setColor(Color.parseColor("#FFE400"));
            dataSet.setValueTextSize(11);
            dataSet.setCircleColor(Color.WHITE);
            dataSet.setCircleColor(Color.parseColor("#FFE400"));
            dataSet.setValueTextColor(Color.parseColor("#998A00"));
            dataSet.setDrawFilled(true);
            if(Utils.getSDKInt() >= 18) {
                dataSet.setFillDrawable(new ColorDrawable(Color.argb(20, 255, 187, 0)));
            } else {
                dataSet.setFillColor(Color.CYAN);
            }
            dataSet.setValueFormatter(new TimeAxisValueFormat());

            lineChart2.clear();
            lineChart2.getXAxis().setValueFormatter(new IndexAxisValueFormatter(timeBox)); //라벨 설정
            lineChart2.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            lineChart2.getXAxis().setTextSize(10);
            lineChart2.getXAxis().setDrawAxisLine(true);
            lineChart2.animateY(500); //애니메이션(0.5초)

            lineChart2.getXAxis().setLabelCount(entriesTime.size(), true);
            lineChart2.getAxisLeft().setValueFormatter(new TimeAxisValueFormat());
            lineChart2.getAxisRight().setDrawLabels(false); //오른쪽 축 숨기기
            lineChart2.getAxisRight().setEnabled(false);
            lineChart2.getAxisLeft().setTextSize(8);
            lineChart2.getAxisLeft().setAxisMinimum(0f);
            lineChart2.getDescription().setEnabled(false); //Description 숨기기
            lineChart2.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            lineChart2.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            lineChart2.setData(new LineData(dataSet)); //차트 데이터 설정
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void updateLineGraph2() {
        try {
            int changed = 0;
            if (!entriesTime.isEmpty()) {
                for (int i = 0; i < timeBox.length; i++) {
                    if (entriesTime.get(i).getY() != timeList.get(i)) {
                        entriesTime.set(i, new Entry(i, timeList.get(i)));
                        changed++;
                    }
                }
                lineChart2.notifyDataSetChanged();
                lineChart2.invalidate();
                if(changed > 0) {
                    lineChart2.animateY(500);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    static class TimeAxisValueFormat extends IndexAxisValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            int hour = (int)value / 60;
            int minutes = (int) (value - (hour * 60));

            return (hour < 10 ? "0" + hour : hour) + ":" + (minutes < 10 ? "0" + minutes : minutes);
        }
    }
}
