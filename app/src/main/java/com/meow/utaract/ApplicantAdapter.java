package com.meow.utaract;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ApplicantAdapter extends RecyclerView.Adapter<ApplicantAdapter.ApplicantViewHolder> {
    private List<Applicant> applicantList;
    private final OnApplicantActionListener listener;

    public interface OnApplicantActionListener {
        void onAccept(Applicant applicant);
        void onReject(Applicant applicant);
        void onViewDetails(Applicant applicant);
    }

    public ApplicantAdapter(List<Applicant> applicantList, OnApplicantActionListener listener) {
        this.applicantList = applicantList;
        this.listener = listener;
    }

    public void updateList(List<Applicant> newList) {
        applicantList.clear();
        applicantList.addAll(newList);
        notifyDataSetChanged(); // Notify the adapter to refresh the UI
    }

    @NonNull
    @Override
    public ApplicantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_applicant, parent, false);
        return new ApplicantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicantViewHolder holder, int position) {
        holder.bind(applicantList.get(position));
    }

    @Override
    public int getItemCount() {
        return applicantList.size();
    }

    class ApplicantViewHolder extends RecyclerView.ViewHolder {
        TextView applicantNameText, statusText;
        Button acceptButton, rejectButton;
        LinearLayout actionButtonsLayout;
        Button viewDetailsButton;

        public ApplicantViewHolder(@NonNull View itemView) {
            super(itemView);
            applicantNameText = itemView.findViewById(R.id.applicantNameText);
            statusText = itemView.findViewById(R.id.statusText);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            actionButtonsLayout = itemView.findViewById(R.id.actionButtonsLayout);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
        }

        void bind(Applicant applicant) {
            applicantNameText.setText(applicant.getUserName());
            statusText.setText("Status: " + applicant.getStatus());

            if ("pending".equals(applicant.getStatus())) {
                actionButtonsLayout.setVisibility(View.VISIBLE);
                viewDetailsButton.setOnClickListener(v -> listener.onViewDetails(applicant));
                acceptButton.setOnClickListener(v -> listener.onAccept(applicant));
                rejectButton.setOnClickListener(v -> listener.onReject(applicant));

            } else {
                actionButtonsLayout.setVisibility(View.GONE);
            }
        }
    }
}