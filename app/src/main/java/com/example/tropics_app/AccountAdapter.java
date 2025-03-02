package com.example.tropics_app;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {
    private final Context context;
    private List<Accounts> userList;
    private OnItemLongClickListener longClickListener;
    public AccountAdapter(Context context, List<Accounts> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_accounts_layout, parent, false);
        return new ViewHolder(view);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Accounts user = userList.get(position);
        holder.tvEmpName.setText(user.getEmail()); // Display user email
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmpName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmpName = itemView.findViewById(R.id.tvEmpName);

            // 🔥 Show Popup Menu on Long Click
            itemView.setOnLongClickListener(v -> {
                showPopupMenu(v, getAdapterPosition());
                return true;
            });
        }
    }


    private void showPopupMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);

        // Set the gravity to the right
        popupMenu.setGravity(Gravity.END);

        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.accounts_item_menu, popupMenu.getMenu());
        Menu menu = popupMenu.getMenu();

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.action_reset_pass) {
                // Call the edit action
                if (longClickListener != null) {
                    longClickListener.onResetPassClick(userList.get(position));
                }
                return true;
            } else if (id == R.id.action_delete) {
                // Add stocks
                if (longClickListener != null) {
                    longClickListener.onDeleteClick(userList.get(position));
                }
                return true;
            } else {
                return false;
            }
        });

        popupMenu.show();
    }

    public interface OnItemLongClickListener {
        void onResetPassClick(Accounts user);
        void onDeleteClick(Accounts user);
    }

}
