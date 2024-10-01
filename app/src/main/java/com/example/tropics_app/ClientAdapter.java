package com.example.tropics_app;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.ClientViewHolder> {

    private List<Client> clients;
    private OnClientClickListener listener;

    public interface OnClientClickListener {
        void onClientSelected(Client client);
    }

    public ClientAdapter(List<Client> clients, OnClientClickListener listener) {
        this.clients = clients;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client, parent, false);
        return new ClientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClientViewHolder holder, int position) {
        Client client = clients.get(position);
        holder.bind(client, listener);
    }

    @Override
    public int getItemCount() {
        return clients.size();
    }

    static class ClientViewHolder extends RecyclerView.ViewHolder {
        TextView tvClientName;

        public ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClientName = itemView.findViewById(R.id.tvClientName);
        }

        public void bind(Client client, OnClientClickListener listener) {
            tvClientName.setText(client.getFirstName() + " " + client.getLastName());
            itemView.setOnClickListener(v -> listener.onClientSelected(client));
        }
    }
}
