package com.rsb.realtimetraffic;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class GraphFragment1 extends Fragment {

    String apiKey = "9731594721";
    String[] regions = {"부산", "서울", "신대구", "광주", "대전", "강원", "대구", "전북", "천안", "충북"};
    String[] cars = {"승용차/미니트럭", "버스", "소형화물차", "중형화물차", "대형화물차"};
    String[] routes = {"경부선","남해선","서해안선","중부선","논산천안선","영동선","수도권제1순환선","제2경인선","경인선"};
    JSONObject json, jsonRoute;
    BarChart barChart, barChart2;
    ArrayList<BarEntry> entries = new ArrayList<>();
    ArrayList<BarEntry> entries2 = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.graph_fragment1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        barChart = view.findViewById(R.id.chart); //지역(권역)별 교통량 그래프
        barChart.setTouchEnabled(false);

        barChart2 = view.findViewById(R.id.chart2); //고속도로별 교통량 그래프
        barChart2.setTouchEnabled(false);

        timerTask();
    }

    public void timerTask() {
        Timer t = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                getTrafficData();
                getRouteTrafficData();
            }
        };
        t.schedule(task, 0, 10000); //10초에 한 번 실행
    }

    public void getTrafficData() {
        new ThreadTask<Integer, Integer>() {
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected Integer doInBackground(Integer arg) {

                try {

                    Document doc = Jsoup.connect("https://data.ex.co.kr/openapi/trafficapi/trafficRegion?key=" + apiKey + "&type=xml&tmType=3")
                            .timeout(3000)
                            .ignoreContentType(true)
                            .get(); //API 호출(한국도로공사)

                    Elements data = doc.select("trafficRegion"); //결과값 목록 불러오기
                    json = new JSONObject(); //JSON 생성

                    for(Element e : data) { //목록을 한 개씩 가져옴
                        String split;
                        //regionName 자르기(부산경남본부 -> 부산, 서울춘천센터 -> 서울...)
                        if(e.select("regionName").text().startsWith("신")) {
                            split = e.select("regionName").text().substring(0, 3);
                        } else if(e.select("regionName").text().contains("고속도로")) {
                            split = e.select("regionName").text();
                        } else {
                            split = e.select("regionName").text().substring(0, 2);
                        }
                        if(!json.has(split)) {
                            if(Arrays.asList(regions).contains(split)) {
                                json.put(split, Integer.valueOf(e.select("trafficAmout").text()));
                            }
                        } else {
                            int val = json.getInt(split) + Integer.valueOf(e.select("trafficAmout").text());
                            json.put(split, val);
                        }
                    }

                } catch(IOException | JSONException e) {
                    e.printStackTrace();
                }

                return 0;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if(entries.isEmpty()) {
                    drawGraph1();
                } else {
                    updateGraph1();
                }
            }
        }.execute(0);
    }

    public void drawGraph1() { //권역(지역)별 교통량 그래프 그리기

        try {
            for(int i = 0; i < regions.length; i++) {
                if(json.has(regions[i])) {
                    int value = json.getInt(regions[i]);
                    entries.add(new BarEntry(i, value));
                }
            }
            BarDataSet dataSet = new BarDataSet(entries, "권역별 교통량(대)");
            dataSet.setHighlightEnabled(true);
            dataSet.setColor(Color.parseColor("#2FED28"));
            dataSet.setValueTextSize(9);
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(regions)); //라벨 설정
            barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            barChart.getXAxis().setTextSize(10);
            barChart.animateY(500); //애니메이션(0.5초)

            barChart.getXAxis().setLabelCount(dataSet.getEntryCount());
            barChart.getAxisRight().setDrawLabels(false); //오른쪽 축 숨기기
            barChart.getAxisRight().setEnabled(false);
            barChart.getAxisLeft().setTextSize(8);
            barChart.getAxisLeft().setAxisMinimum(0f);
            barChart.getDescription().setEnabled(false); //Description 숨기기
            barChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            barChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            barChart.setData(new BarData(dataSet)); //차트 데이터 설정

        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateGraph1() {
        try {
            int changed = 0; //몇 개의 데이터가 변동 되었는지 확인하기 위한 변수
            if (!entries.isEmpty()) {
                for (int i = 0; i < regions.length; i++) {
                    if (entries.get(i).getY() != json.getInt(regions[i])) {
                        entries.set(i, new BarEntry(i, json.getInt(regions[i])));
                        changed++;
                    }
                }
                barChart.notifyDataSetChanged();
                barChart.invalidate();
                if(changed > 0) {
                    barChart.animateY(500);
                }
            }
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public void getRouteTrafficData() {
        new ThreadTask<Integer, Integer>() {
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected Integer doInBackground(Integer arg) {

                try {
                    Document doc = Jsoup.connect("https://data.ex.co.kr/openapi/odtraffic/trafficAmountByCongest?key=" + apiKey + "&type=xml&tmType=3")
                            .timeout(3000)
                            .ignoreContentType(true)
                            .get(); //API 호출(한국도로공사)

                    Elements data = doc.select("list"); //결과값 목록 불러오기
                    jsonRoute = new JSONObject(); //JSON 생성

                    for(Element e : data) { //목록을 한 개씩 가져옴
                        String split= "";
                        //고속도로명 분류하기(남해선, 경부선 등)
                        split = e.select("routeName").text();
                        if(split.contains("(")) {
                            split = split.substring(0, split.indexOf("(")).trim();
                        }
                        if(!jsonRoute.has(split)) {
                            if(Arrays.asList(routes).contains(split)) {
                                jsonRoute.put(split, Integer.valueOf(e.select("trafficAmout").text()));
                            }
                        } else {
                            int val = jsonRoute.getInt(split) + Integer.valueOf(e.select("trafficAmout").text());
                            jsonRoute.put(split, val);
                        }
                    }

                } catch(IOException | JSONException e) {
                    e.printStackTrace();
                }

                return 0;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if(entries2.isEmpty()) {
                    drawGraph2();
                } else {
                    updateGraph2();
                }
            }
        }.execute(0);
    }

    public void drawGraph2() { //고속도로별 교통량 그래프 그리기

        try {
            for(int i = 0; i < routes.length; i++) {
                if(jsonRoute.has(routes[i])) {
                    int value = jsonRoute.getInt(routes[i]);
                    entries2.add(new BarEntry(i, value));
                } else {
                    entries2.add(new BarEntry(i, 0));
                }
            }
            BarDataSet dataSet = new BarDataSet(entries2, "고속도로별 교통량(대)");
            dataSet.setHighlightEnabled(true);
            dataSet.setColor(Color.parseColor("#00D8FF"));
            dataSet.setValueTextSize(11);
            barChart2.getXAxis().setValueFormatter(new IndexAxisValueFormatter(routes)); //라벨 설정
            barChart2.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            barChart2.getXAxis().setLabelRotationAngle(-30);
            barChart2.getXAxis().setTextSize(8);
            barChart2.animateY(500); //애니메이션(0.5초)

            barChart2.getXAxis().setLabelCount(dataSet.getEntryCount());
            barChart2.getAxisRight().setDrawLabels(false); //오른쪽 축 숨기기
            barChart2.getAxisRight().setEnabled(false);
            barChart2.getAxisLeft().setTextSize(8);
            barChart2.getAxisLeft().setAxisMinimum(0f);
            barChart2.getAxisLeft().setAxisMaximum(3000f);
            barChart2.getDescription().setEnabled(false); //Description 숨기기
            barChart2.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            barChart2.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
            barChart2.setData(new BarData(dataSet)); //차트 데이터 설정

        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateGraph2() {
        try {
            int changed = 0;
            if (!entries2.isEmpty()) {
                for (int i = 0; i < routes.length; i++) {
                    if (jsonRoute.has(routes[i])) {
                        if (entries2.get(i).getY() != jsonRoute.getInt(routes[i])) {
                            entries2.set(i, new BarEntry(i, jsonRoute.getInt(routes[i])));
                            changed++;
                        }
                    }
                }
                barChart2.notifyDataSetChanged();
                barChart2.invalidate();
                if(changed > 0) {
                    barChart2.animateY(500);
                }
            }
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }
}
