package com.example.incentive_spirometer_and_dvt_application.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.incentive_spirometer_and_dvt_application.R;
import com.example.incentive_spirometer_and_dvt_application.helpers.DatabaseHelper;
import com.example.incentive_spirometer_and_dvt_application.models.Dvt;
import com.example.incentive_spirometer_and_dvt_application.models.DvtData;
import com.example.incentive_spirometer_and_dvt_application.models.IncentiveSpirometer;
import com.example.incentive_spirometer_and_dvt_application.models.IncentiveSpirometerData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class DvtFragment extends Fragment{
    static final String TAG = "PatientDvtInfoFragment";
    private DatabaseHelper databaseHelper;

    private List<DvtData> allDvtData;
    private List<BarEntry> allBarEntries;

    private Spinner dataWindowSpinner;

    private int numOfDaysInt;

    private List<BarEntry> shownEntries;
    private BarChart graph;
    private TextView noDvtTextView;
    private GridLayout columnTitlesGridLayout;
    private LinearLayout dvtInfoSpinnerArea;

    private ListView dataListView;

    private int patientId;
    private int doctorId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            patientId = bundle.getInt("patientId", -1);
            doctorId = bundle.getInt("doctorId", -1);
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart: ONSTART");
        super.onStart();
        createDataLists();
        drawGraph();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: ONERESUME");
        super.onResume();
        createDataLists();
        drawGraph();

        checkForNoDevice();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.activity_patient_dvt_info, container, false);

        dataWindowSpinner = (Spinner) view.findViewById(R.id.dataWindowSpinner);
        ArrayList<String> numOfDays = new ArrayList<>();
        numOfDays.add("1");
        numOfDays.add("2");
        numOfDays.add("3");
        numOfDays.add("4");
        numOfDays.add("5");
        numOfDays.add("6");
        numOfDays.add("7");
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_dropdown_item, numOfDays);//this.getContext(), android.R.layout.simple_spinner_dropdown_item, );
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataWindowSpinner.setAdapter(arrayAdapter);
        dataWindowSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String numOfDays = parent.getItemAtPosition(position).toString();
                numOfDaysInt = Integer.parseInt(numOfDays);
                setDataWindow(24 * numOfDaysInt);
                drawGraph();
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });

        graph = (BarChart) view.findViewById((R.id.patient_dvt_graph));
        dataListView = (ListView) view.findViewById(R.id.patient_dvt_table);

        databaseHelper = new DatabaseHelper(getContext());

        shownEntries = new ArrayList<>();

        noDvtTextView = view.findViewById(R.id.noDvtTextView);
        columnTitlesGridLayout = view.findViewById(R.id.dvt_column_titles);
        dvtInfoSpinnerArea = view.findViewById(R.id.dvtInfoSpinnerArea);

        createDataLists();
        drawGraph();

        return view;
    }

    private void checkForNoDevice() {
        Dvt dvt = databaseHelper.getDvt(patientId);

        if (dvt == null) {
            noDvtTextView.setVisibility(View.VISIBLE);
            dataListView.setVisibility(View.GONE);
            columnTitlesGridLayout.setVisibility(View.GONE);
            graph.setVisibility(View.GONE);
            dvtInfoSpinnerArea.setVisibility(View.GONE);
        } else {
            noDvtTextView.setVisibility(View.GONE);
            dataListView.setVisibility(View.VISIBLE);
            columnTitlesGridLayout.setVisibility(View.VISIBLE);
            graph.setVisibility(View.VISIBLE);
            dvtInfoSpinnerArea.setVisibility(View.VISIBLE);
        }
    }

    /*
    gets the data for display from the database, sorts it into the different lists for display
     */
    private void createDataLists() {
        allDvtData = new ArrayList<>();
        allBarEntries = new ArrayList<>();

        allDvtData = databaseHelper.getPatinetDvtData(patientId);

        Collections.sort(allDvtData);

        // date for use with test data only - will need to be updated to reflect the CURRENT DATE when in real use
        Calendar now = new GregorianCalendar(2019, Calendar.NOVEMBER, 11, 7, 0, 0);

        for (int session = 1; session <= allDvtData.size(); session++) {
            DvtData dvtd = allDvtData.get(session - 1);
            allBarEntries.add(new BarEntry(session, new float[] {dvtd.getRepsCompleted(), dvtd.getNumberOfReps() - dvtd.getRepsCompleted()}));
        }

        setDataWindow(24);

        ArrayAdapter<DvtData> arrayAdapter = new ArrayAdapter<DvtData>(getContext(),
                R.layout.dvt_info_list_row, R.id.row_session, allDvtData) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Collections.sort(allDvtData, Collections.<DvtData>reverseOrder());

                View view = super.getView(position, convertView, parent);

                TextView session = (TextView) view.findViewById(R.id.row_session);
                TextView start = (TextView) view.findViewById(R.id.row_start);
                TextView end = (TextView) view.findViewById(R.id.row_end);
                TextView resistance = (TextView) view.findViewById(R.id.resistance_dvt_table_row);
                TextView breaths_completed_ratio = (TextView) view.findViewById(R.id.row_ex_complete_ratio);

                String breath_ratio_string = allDvtData.get(position).getRepsCompleted() + " / " + allDvtData.get(position).getNumberOfReps();

                session.setText(String.format(String.format("%s",allDvtData.size() - position)));
                start.setText(allDvtData.get(position).getStringTime("start"));
                end.setText(allDvtData.get(position).getStringTime("end"));
                resistance.setText(allDvtData.get(position).getResistance());

                breaths_completed_ratio.setText(breath_ratio_string);

                return view;
            }
        };
        dataListView.setAdapter(arrayAdapter);
    }


    // sets the amount of data that will be shown on the graph
    // hoursToShow: the number of past hours the user would like to see, for example, if the user
    // would like to see data from the past day this number should be 24
    private void setDataWindow(int hoursToShow) {
        Collections.sort(allDvtData);

        for (int session = 1; session <= allDvtData.size(); session++) {
            DvtData dvtd = allDvtData.get(session - 1);
            float ex_rate = (float) ((double)dvtd.getRepsCompleted()*3600.0/(double) (TimeUnit.MILLISECONDS.toSeconds(dvtd.getEndTime().getTime() - dvtd.getStartTime().getTime())));
            allBarEntries.add(new BarEntry(session, new float[] {ex_rate, dvtd.getNumberOfReps() - dvtd.getRepsCompleted()}));
        }

        shownEntries.clear();
        Calendar now = new GregorianCalendar();
        for (int session = 1; session <= allDvtData.size(); session++) {
            DvtData dvtd = allDvtData.get(session - 1);
            int timeDiff = (int) (TimeUnit.MILLISECONDS.toHours(now.getTimeInMillis() - dvtd.getStartTime().getTime()));
            if (timeDiff < hoursToShow) {
                shownEntries.add(allBarEntries.get(session - 1));
            }
        }
        for (int session = shownEntries.size(); session < numOfDaysInt * 10; session++){
            shownEntries.add(new BarEntry(session, 0));
        }
    }

    // draws the features of the graph, including removing the description and legend, setting
    // touch selection to enabled, setting the data set to the graph, and setting all labels
    // visible for the graph and bars on the graph
    private void drawGraph() {
        graph.getDescription().setEnabled(false);
        graph.getLegend().setEnabled(false);
        graph.setTouchEnabled(true);

        BarDataSet set = new BarDataSet(shownEntries, "BarDataSet");
        int completedColor = getResources().getColor(R.color.colorAccent);
        int uncompleteColor = getResources().getColor(R.color.colorEmptyBar);
        set.setColors(completedColor, uncompleteColor);
        BarData data = new BarData(set);

        // will individually label bars in the graph if removed - good for testing bar overlap
        set.setDrawValues(false);

        XAxis x = graph.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setLabelRotationAngle(-90);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(true);
        // x.setDrawLabels(false);

        x.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                return super.getBarLabel(barEntry);
            }
        });


        YAxis yleft = graph.getAxisLeft();
        graph.getAxisRight().setEnabled(false);

        yleft.setAxisMinimum(0);
        yleft.setAxisMaximum(12);

        IMarker marker = new dvt_graph_labels(getContext(), R.layout.dvt_graph_labels, allDvtData);
        graph.setMarker(marker);
        graph.setScaleEnabled(false);

        graph.setData(data);
        graph.invalidate();
    }
}
