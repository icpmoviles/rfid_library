package es.icp.pistola.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;

import java.util.ArrayList;

import es.icp.logs.core.MyLog;
import es.icp.pistola.R;
import es.icp.pistola.RfidGlobalVariables;

public class ReaderListAdapter extends ArrayAdapter<ReaderDevice> {
    private final ArrayList<ReaderDevice> readersList;
    private final Context context;
    private final int resourceId;

    public ReaderListAdapter(Context context, int resourceId, ArrayList<ReaderDevice> readersList) {
        super(context, resourceId, readersList);
        this.context = context;
        this.resourceId = resourceId;
        this.readersList = readersList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ReaderDevice reader = readersList.get(position);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(resourceId, null);
        }
        CheckedTextView checkedTextView = (CheckedTextView) convertView.findViewById(R.id.reader_checkedtextview);
        checkedTextView.setText(reader.getName() + "\n" + reader.getAddress());

        LinearLayout readerDetail = (LinearLayout) convertView.findViewById(R.id.reader_detail);
        RFIDReader rfidReader = reader.getRFIDReader();
        MyLog.d("Pistola " + rfidReader.getHostName() + " - conectada: " + rfidReader.isConnected());
//        if (rfidReader != null && rfidReader.isConnected()) {
        if (rfidReader != null && RfidGlobalVariables.CONNECTED_READER != null && rfidReader.getHostName().equals(RfidGlobalVariables.CONNECTED_READER.getHostName())) {
            checkedTextView.setChecked(true);
            readerDetail.setVisibility(View.VISIBLE);
            if (RfidGlobalVariables.CONNECTED_READER != null
                    && RfidGlobalVariables.CONNECTED_READER.ReaderCapabilities != null
                    && RfidGlobalVariables.CONNECTED_READER.ReaderCapabilities.getModelName() != null
                    && RfidGlobalVariables.CONNECTED_READER.ReaderCapabilities.getSerialNumber() != null) {
                ((TextView) convertView.findViewById(R.id.tv_model)).setText(RfidGlobalVariables.CONNECTED_READER.ReaderCapabilities.getModelName());
                ((TextView) convertView.findViewById(R.id.tv_serial)).setText(RfidGlobalVariables.CONNECTED_READER.ReaderCapabilities.getSerialNumber());
            }

        } else {
            readerDetail.setVisibility(View.GONE);
            checkedTextView.setChecked(false);
        }
        return convertView;
    }
}
