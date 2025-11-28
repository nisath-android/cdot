package org.naminfo.activities.main.ptt.pttcontacts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.naminfo.R
import org.naminfo.activities.main.fragments.SecureFragment
import org.naminfo.activities.main.ptt.PTTFragment
import org.naminfo.activities.main.ptt.pttcontacts.adapter.PttContactsItem
import org.naminfo.activities.main.ptt.pttrecents.adapter.PttContactsAdapter
import org.naminfo.activities.main.ptt.pttvoice.PTTVoiceFragment
import org.naminfo.databinding.FragmentPttContactsBinding

class PTTContactsFragment : SecureFragment<FragmentPttContactsBinding>() {

    companion object {
        fun newInstance() = PTTContactsFragment()
    }

    private val viewModel: PTTContactsViewModel by viewModels()
    override fun getLayoutId(): Int {
        return R.layout.fragment_ptt_contacts
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        setRecyclerView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        setRecyclerView()
    }
    private fun setRecyclerView() {
        val messageList = listOf(
            PttContactsItem(
                "CDot-1",
                "9874563211",
                "https://ui-avatars.com/api/?name=Nisath+P&background=0D8ABC&color=fff&size=128"
            ),
            PttContactsItem(
                "CDot-2",
                "9874563212",
                "https://ui-avatars.com/api/?name=Dhivya+P&background=0D8ABC&color=fff&size=128"
            ),
            PttContactsItem(
                "CDot-11",
                "1001",
                "https://ui-avatars.com/api/?name=NAMJio+P&background=0D8ABC&color=fff&size=128"
            ),
            PttContactsItem(
                "CDot-12",
                "1002",
                "https://ui-avatars.com/api/?name=Magesh+P&background=0D8ABC&color=fff&size=128"
            )

        )

        val adapter = PttContactsAdapter(
            viewModel,
            requireContext(),
            items = messageList,
            onItemClick = { item ->
                Toast.makeText(context, "Clicked ${item.name}", Toast.LENGTH_SHORT).show()
                loadFragment(item)
            },
            onMessageIconClick = { item ->
                Toast.makeText(context, "Message to ${item.name}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadFragment(item: PttContactsItem) {
        val fragment = PTTVoiceFragment.newInstance(item.name, item.phone, item.profileImageUrl)
        (parentFragment as? PTTFragment)?.replaceWithVoiceFragment(fragment)
    }
}
