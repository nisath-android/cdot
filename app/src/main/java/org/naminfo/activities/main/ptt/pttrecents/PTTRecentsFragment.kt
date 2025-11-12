package org.naminfo.activities.main.ptt.pttrecents

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.naminfo.R
import org.naminfo.activities.main.fragments.SecureFragment
import org.naminfo.activities.main.ptt.PTTFragment
import org.naminfo.activities.main.ptt.pttrecents.adapter.PttRecentAdapter
import org.naminfo.activities.main.ptt.pttrecents.adapter.PttRecentItem
import org.naminfo.activities.main.ptt.pttvoice.PTTVoiceFragment
import org.naminfo.databinding.FragmentPttRecentsBinding

class PTTRecentsFragment : SecureFragment<FragmentPttRecentsBinding>() {

    companion object {
        fun newInstance() = PTTRecentsFragment()
    }

    private val viewModel: PTTRecentsViewModel by viewModels()
    override fun getLayoutId(): Int {
        return R.layout.fragment_ptt_recents
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        setRecyclerView()
    }

    private fun setRecyclerView() {
        val messageList = listOf<PttRecentItem>()

        val adapter = PttRecentAdapter(
            viewModel,
            requireContext(),
            items = messageList,
            onItemClick = { item ->
                Toast.makeText(context, "Clicked ${item.name}", Toast.LENGTH_SHORT).show()
                loadFragment(PTTVoiceFragment())
            },
            onMessageIconClick = { item ->
                Toast.makeText(context, "Message to ${item.name}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }
    private fun loadFragment(fragment: Fragment) {
        (parentFragment as? PTTFragment)?.replaceWithVoiceFragment(fragment)
    }
}
