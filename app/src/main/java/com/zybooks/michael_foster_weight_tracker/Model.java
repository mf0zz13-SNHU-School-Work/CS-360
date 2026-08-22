package com.zybooks.michael_foster_weight_tracker;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides application logic that supports account creation and weight displays.
 *
 * <p>The controller calls this class to coordinate database results with the
 * weight-tracking screen, including previous weigh-ins, today's weight, and the
 * graph view.</p>
 */
public class Model{

    /**
     * Creates a new user account and stores the user's goal weight.
     *
     * @param database database helper used to save account and goal records
     * @param userName username for the new account
     * @param password password for the new account
     * @param goalWeight goal weight entered by the user
     * @return true when both the user and goal records are saved, otherwise false
     */
    public boolean createAccount(
            WeightTrackingDatabase database,
            String userName,
            String password,
            double goalWeight
    ){
        try {
            boolean updateUserTable = database.addUser(userName, password);
            boolean updateGoalWeight = database.addGoalWeight(userName, goalWeight);

            if (updateUserTable && updateGoalWeight) { return true;}
        } catch (Exception e) {
            Log.d("CreateNewAccount", "Error creating user", e);
            return false;
        }
        return false;
    }

    /**
     * Displays previous weigh-ins as rows in the weight tracking screen.
     *
     * <p>Each row includes the weigh-in date, weight value, and a delete button
     * that refreshes the displayed list, today's weight banner, and graph.</p>
     *
     * @param userName username whose weigh-ins should be displayed
     * @param database database helper used to retrieve and delete weigh-ins
     * @param weighInCardContainer layout that receives generated weigh-in rows
     * @param lbsToGo text view used for the goal-progress banner
     * @param todayWeight input field used to display today's saved weight
     * @param graph graph view refreshed after row deletion
     * @param context Android context used for resources and view creation
     */
    public void displayPreviousWeights(
            String userName,
            WeightTrackingDatabase database,
            LinearLayout weighInCardContainer,
            TextView lbsToGo,
            EditText todayWeight,
            GraphView graph,
            Context context) {

        String toGo = context.getString(R.string.poundsToGo);
        String lbs = context.getString(R.string.lbs);
        String delete = context.getString(R.string.delete);
        int red = context.getColor(R.color.delete_red);
        int white = context.getColor(R.color.white);

        Map<String, Double> weightIns = database.getWeighIns(userName);

        weighInCardContainer.removeAllViews();

        for(Map.Entry<String,Double> entry : weightIns.entrySet()) {
            String date = entry.getKey();
            double weight = entry.getValue();

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 12, 0, 12);

            LinearLayout.LayoutParams columnParams =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    );

            TextView dateView = new TextView(context);
            dateView.setText(date);
            dateView.setTextSize(18);
            dateView.setGravity(Gravity.CENTER);

            TextView weightView = new TextView(context);
            weightView.setText(String.format(Locale.ENGLISH,"%.2f %s",weight,lbs));
            weightView.setTextSize(18);
            weightView.setGravity(Gravity.CENTER);

            Button deleteButton = new Button(context);
            deleteButton.setBackgroundColor(red);
            deleteButton.setTextColor(white);
            deleteButton.setText(delete);

            deleteButton.setOnClickListener(view ->{
                boolean deleted = database.deleteWeight(userName, date);
                displayPreviousWeights(userName, database,weighInCardContainer,lbsToGo,todayWeight,graph,context);
                checkForTodayWeight(userName,database,lbsToGo,todayWeight,context);
                displayWeightGraph(userName,database,graph, context);
                if (!deleted) {
                    Log.d("Delete record", "Invalid date for graph");
                }
            });

            row.addView(dateView, columnParams);
            row.addView(weightView, columnParams);
            row.addView(deleteButton, columnParams);

            weighInCardContainer.addView(row);
        }
    }

    /**
     * Displays today's saved weight and goal-progress message when available.
     *
     * @param userName username whose current-day weight should be checked
     * @param database database helper used to retrieve today's and goal weights
     * @param banner text view that displays the pounds-to-go message
     * @param enterWeight input field that displays today's saved weight
     * @param context Android context used to retrieve string resources
     */
    public void checkForTodayWeight(
            String userName,
            WeightTrackingDatabase database,
            TextView banner,
            EditText enterWeight,
            Context context){

        double todayWeight = database.getTodayWeight(userName);
        double goalWeight = database.getGoalWeight(userName);
        double poundsToGo = todayWeight - goalWeight;

        String toGo = context.getString(R.string.poundsToGo);
        String lbs = context.getString(R.string.lbs);

        if (todayWeight == -1.0) {
            enterWeight.setText("");
            banner.setText("");
            banner.setEnabled(false);
            return;
        }
        else {
            banner.setEnabled(true);
            banner.setText(String.format(
                    Locale.ENGLISH,
                    "%.2f %s %.2f %s",
                    poundsToGo,
                    toGo,
                    goalWeight,
                    lbs.toLowerCase()
            ));

            enterWeight.setText(String.format(Locale.ENGLISH,"%.2f",todayWeight));

        }
    }

    /**
     * Displays the user's weigh-ins on the graph view.
     *
     * <p>The method converts stored date strings to graph data points, sorts them
     * chronologically, and configures the graph axes to fit the available data.</p>
     *
     * @param userName username whose weigh-ins should be graphed
     * @param database database helper used to retrieve weigh-in history
     * @param graph graph view that displays the weight history
     * @param context Android context used by the graph date label formatter
     */
    public void displayWeightGraph(
            String userName,
            WeightTrackingDatabase database,
            GraphView graph,
            Context context) {

        Map<String, Double> weighIns = database.getWeighIns(userName);

        graph.removeAllSeries();

        if (weighIns.isEmpty()) {
            return;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        List<DataPoint> points = new ArrayList<>();

        for (Map.Entry<String, Double> entry : weighIns.entrySet()) {
            try {
                Date date = formatter.parse(entry.getKey());
                double weight = entry.getValue();

                points.add(new DataPoint(date, weight));
            } catch (Exception e) {
                Log.d("Weight Graph", "Invalid date for graph", e);
            }
        }

        if (points.isEmpty()) {
            return;
        }

        Collections.sort(points, (a, b) -> Double.compare(a.getX(), b.getX()));

        LineGraphSeries<DataPoint> series =
                new LineGraphSeries<>(points.toArray(new DataPoint[0]));

        series.setDrawDataPoints(true);
        series.setDataPointsRadius(8);
        series.setThickness(6);

        graph.addSeries(series);

        graph.getGridLabelRenderer().setLabelFormatter(
                new DateAsXAxisLabelFormatter(context, formatter)
        );
        graph.getGridLabelRenderer().setLabelVerticalWidth(90);
        graph.getGridLabelRenderer().setHumanRounding(false);
        graph.getGridLabelRenderer().setNumHorizontalLabels(3);

        double oneDay = 24 * 60 * 60 * 1000;

        double minX = points.get(0).getX();
        double maxX = points.get(points.size() - 1).getX();

        graph.getViewport().setXAxisBoundsManual(true);

        if (points.size() == 1) {
            graph.getViewport().setMinX(minX - oneDay);
            graph.getViewport().setMaxX(maxX + oneDay);
        } else {
            graph.getViewport().setMinX(minX);
            graph.getViewport().setMaxX(maxX);
        }

        double minY = points.get(0).getY();
        double maxY = points.get(0).getY();

        for (DataPoint point : points) {
            minY = Math.min(minY, point.getY());
            maxY = Math.max(maxY, point.getY());
        }

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(minY - 5);
        graph.getViewport().setMaxY(maxY + 5);
    }
}
