package com.meow.utaract;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import androidx.core.content.ContextCompat;

public class ApplicantAdapter extends RecyclerView.Adapter<ApplicantAdapter.ApplicantViewHolder> {

    private final List<Applicant> applicantList = new ArrayList<>();
    private final OnApplicantActionListener listener;

    public interface OnApplicantActionListener {
        void onAccept(Applicant applicant);
        void onReject(Applicant applicant);
        void onViewDetails(Applicant applicant);
    }

    public ApplicantAdapter(List<Applicant> applicantList, OnApplicantActionListener listener) {
        this.applicantList.addAll(applicantList);
        this.listener = listener;
    }

    public void updateList(List<Applicant> newList) {
        applicantList.clear();
        applicantList.addAll(newList);
        notifyDataSetChanged();
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
        Button acceptButton, rejectButton, viewDetailsButton;
        LinearLayout actionButtonsLayout;

        public ApplicantViewHolder(@NonNull View itemView) {
            super(itemView);
            applicantNameText = itemView.findViewById(R.id.applicantNameText);
            statusText = itemView.findViewById(R.id.statusText);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
            actionButtonsLayout = itemView.findViewById(R.id.actionButtonsLayout);
        }

        void bind(Applicant applicant) {
            applicantNameText.setText(applicant.getUserName());
            statusText.setText(applicant.getStatus());

            // Set status color
            Context context = itemView.getContext();
            int colorRes;
            switch (applicant.getStatus()) {
                case "accepted":
                    colorRes = R.color.status_accepted_text;
                    break;
                case "rejected":
                    colorRes = R.color.status_rejected_text;
                    break;
                default: // pending
                    colorRes = R.color.status_pending_text;
                    break;
            }
            statusText.setTextColor(ContextCompat.getColor(context, colorRes));

            // This listener is always active, regardless of status
            viewDetailsButton.setOnClickListener(v -> listener.onViewDetails(applicant));

            // Show/Hide the Accept/Reject buttons based on status
            if ("pending".equals(applicant.getStatus())) {
                actionButtonsLayout.setVisibility(View.VISIBLE);
                acceptButton.setOnClickListener(v -> listener.onAccept(applicant));
                rejectButton.setOnClickListener(v -> listener.onReject(applicant));
            } else {
                actionButtonsLayout.setVisibility(View.GONE);
            }
        }
    }
}