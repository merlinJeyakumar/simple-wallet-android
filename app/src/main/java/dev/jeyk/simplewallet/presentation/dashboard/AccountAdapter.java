package dev.jeyk.simplewallet.presentation.dashboard;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import dev.jeyk.simplewallet.R;
import dev.jeyk.simplewallet.databinding.ItemAccountBinding;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.presentation.common.WalletFormatters;

public final class AccountAdapter
        extends ListAdapter<WalletAccount, AccountAdapter.AccountViewHolder> {
    public interface AccountClickListener {
        void onAccountClicked(WalletAccount account);
    }

    private static final DiffUtil.ItemCallback<WalletAccount> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull WalletAccount oldItem,
                        @NonNull WalletAccount newItem
                ) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull WalletAccount oldItem,
                        @NonNull WalletAccount newItem
                ) {
                    return oldItem.equals(newItem);
                }
            };

    private final AccountClickListener clickListener;

    public AccountAdapter(AccountClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = Objects.requireNonNull(clickListener, "clickListener");
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAccountBinding binding = ItemAccountBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new AccountViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId().hashCode();
    }

    static final class AccountViewHolder extends RecyclerView.ViewHolder {
        private final ItemAccountBinding binding;

        AccountViewHolder(ItemAccountBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WalletAccount account, AccountClickListener clickListener) {
            String currencyCode = account.getCurrency().getCurrencyCode();
            String formattedBalance = WalletFormatters.money(
                    account.getBalance(),
                    account.getCurrency()
            );
            binding.accountName.setText(account.getName());
            binding.accountCurrency.setText(currencyCode);
            binding.accountBalance.setText(formattedBalance);
            binding.accountCard.setContentDescription(binding.getRoot().getContext().getString(
                    R.string.account_content_description,
                    account.getName(),
                    account.getMaskedNumber(),
                    formattedBalance
            ));
            binding.accountCard.setOnClickListener(ignored -> clickListener.onAccountClicked(account));
        }
    }
}
