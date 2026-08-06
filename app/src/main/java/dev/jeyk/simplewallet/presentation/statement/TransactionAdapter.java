package dev.jeyk.simplewallet.presentation.statement;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Currency;

import dev.jeyk.simplewallet.R;
import dev.jeyk.simplewallet.databinding.ItemTransactionBinding;
import dev.jeyk.simplewallet.domain.model.AccountStatement;
import dev.jeyk.simplewallet.domain.model.TransactionType;
import dev.jeyk.simplewallet.domain.model.WalletTransaction;
import dev.jeyk.simplewallet.presentation.common.WalletFormatters;

public final class TransactionAdapter
        extends ListAdapter<WalletTransaction, TransactionAdapter.TransactionViewHolder> {
    private static final DiffUtil.ItemCallback<WalletTransaction> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull WalletTransaction oldItem,
                        @NonNull WalletTransaction newItem
                ) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull WalletTransaction oldItem,
                        @NonNull WalletTransaction newItem
                ) {
                    return oldItem.equals(newItem);
                }
            };

    private Currency currency;

    public TransactionAdapter() {
        super(DIFF_CALLBACK);
        setHasStableIds(true);
    }

    public void submitStatement(AccountStatement statement) {
        currency = statement.getAccount().getCurrency();
        submitList(statement.getTransactions());
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        if (currency == null) {
            throw new IllegalStateException("Statement currency must be set before binding");
        }
        holder.bind(getItem(position), currency);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId().hashCode();
    }

    static final class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WalletTransaction transaction, Currency currency) {
            boolean credit = transaction.getType() == TransactionType.CREDIT;
            String type = binding.getRoot().getContext().getString(
                    credit ? R.string.transaction_type_credit : R.string.transaction_type_debit
            );
            String amount = WalletFormatters.signedMoney(
                    transaction.getAmount(),
                    currency,
                    transaction.getType()
            );
            String timestamp = WalletFormatters.dateTime(transaction.getOccurredAt());
            String balanceAfter = WalletFormatters.money(transaction.getBalanceAfter(), currency);

            binding.transactionDescription.setText(transaction.getDescription());
            binding.transactionAmount.setText(amount);
            binding.transactionAmount.setTextColor(binding.getRoot().getContext().getColor(
                    credit ? R.color.wallet_positive : R.color.wallet_text_primary
            ));
            binding.transactionTimestamp.setText(timestamp);
            binding.transactionType.setText(type);
            binding.transactionType.setTextColor(binding.getRoot().getContext().getColor(
                    credit ? R.color.wallet_positive : R.color.wallet_text_secondary
            ));
            binding.transactionType.setBackgroundResource(
                    credit ? R.drawable.bg_wallet_credit_badge : R.drawable.bg_wallet_badge
            );
            binding.transactionBalanceAfter.setText(binding.getRoot().getContext().getString(
                    R.string.transaction_balance_after,
                    balanceAfter
            ));
            binding.getRoot().setContentDescription(binding.getRoot().getContext().getString(
                    R.string.transaction_content_description,
                    transaction.getDescription(),
                    timestamp,
                    type,
                    amount,
                    balanceAfter
            ));
        }
    }
}
