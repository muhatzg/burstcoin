/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nxt;

import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import nxt.util.Convert;
import nxt.util.Logger;



/**
 *
 * @author rainman
 */
public class DDosProtection {
    public static List<TransactionImpl> sanitizeTransactionList(Collection<TransactionImpl> transactionList)
    {
        return sanitizeTransactionList(transactionList, "Unknown");
    }
    
    public static List<TransactionImpl> sanitizeTransactionList(Collection<TransactionImpl> transactionList, String actor)
    {
        int numTransactions = transactionList.size();
        List<TransactionImpl> discardedTransactions = new ArrayList<>();

        if(numTransactions > Constants.NUM_UNCONFIRMED_TRANSACTIONS_INVESTIGATE) //Something fishy seems to be happening. Investigate
        {
            HashMap<Long, Integer> limitMap = new HashMap<>();
            int totalMessageBytes = 0;
            boolean discardGlobalMessageWarning = false;
            boolean discardIndividualMessageWarning = false;
            
            for(Iterator<TransactionImpl> transactionIterator = transactionList.iterator(); transactionIterator.hasNext();)
            {
                TransactionImpl transaction = transactionIterator.next();
                long senderId = transaction.getSenderId();
                if(transaction.getMessage() != null && transaction.getAmountNQT() == 0 && limitMap.containsKey(senderId))
                {
                    int totalSenderMessageBytes = limitMap.get(senderId);
                    if(totalSenderMessageBytes > Constants.MAX_SENDER_MESSEGAE_BYTES)
                    {
                        if(!discardIndividualMessageWarning)
                        {
                            Logger.logInfoMessage(actor + ": Discarding all further messages for address: " + Convert.rsAccount(senderId) 
                                    + " because it has more than " + Constants.MAX_SENDER_MESSEGAE_BYTES + " bytes in pending");
                            discardIndividualMessageWarning = true;
                        }
                        transactionIterator.remove();
                        discardedTransactions.add(transaction);
                    }

                    limitMap.put(senderId, totalSenderMessageBytes + transaction.getMessage().getMySize());
                    continue;
                }
                else if (transaction.getMessage() != null)
                {
                    limitMap.put(senderId,transaction.getMessage().getMySize());
                }

                if(transaction.getMessage() != null)
                {
                    totalMessageBytes += transaction.getMessage().getMySize();
                }

                if(totalMessageBytes > Constants.TOTAL_FORWARDED_MESSAGE_BYTES)
                {
                    if(!discardGlobalMessageWarning)
                    {
                        Logger.logInfoMessage(actor + ": Total number of message transactions exceeded. Discarding old messages to prevent network flooding");
                        discardGlobalMessageWarning = true;
                    }
                    transactionIterator.remove();
                    discardedTransactions.add(transaction);
                }    
            }
        }
        if(discardedTransactions.size() > 0)
        {
            Logger.logInfoMessage(actor + ": Discarded a total of " + discardedTransactions.size() 
                    + " message transactions. TransactionList before: " + numTransactions + 
                    ", after: " + transactionList.size());
        }
        return discardedTransactions;
    }
    
    
    //This function tries to ensure that certain limits are kept in total number of block messages
    //and also payload bytes of messages. This should prevent a massive attack of large messages
    //from drowning out valid transactions
    public static void preSortBlockTransactions(List<TransactionImpl> transactions)
    {
        
    }
}
