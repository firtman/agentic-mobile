import AVFAudio

enum GameCue: String, CaseIterable {
    case correct, incorrect, timeout, swap, completion
}

@MainActor
final class GameSound {
    private var players: [GameCue: AVAudioPlayer] = [:]
    private var configured = false

    func play(_ cue: GameCue) {
        let enabled = UserDefaults.standard.object(forKey: SettingKey.gameSound) as? Bool ?? true
        guard enabled else { return }
        do {
            // Ambient mixes with other audio and respects the Ring/Silent switch.
            let first = !configured
            if first {
                try AVAudioSession.sharedInstance().setCategory(.ambient)
                configured = true
            }
            guard let player = try player(for: cue) else { return }
            player.currentTime = 0
            player.play()
            if first {
                // Load the remaining cues after this one starts, so later cues do not pay setup cost.
                Task { [weak self] in
                    for other in GameCue.allCases { _ = try? self?.player(for: other) }
                }
            }
        } catch {
            // Audio is optional. Game transitions always finish.
        }
    }

    private func player(for cue: GameCue) throws -> AVAudioPlayer? {
        if let player = players[cue] { return player }
        guard let url = Bundle.main.url(forResource: "cue-\(cue.rawValue)", withExtension: "wav") else { return nil }
        let player = try AVAudioPlayer(contentsOf: url)
        player.prepareToPlay()
        players[cue] = player
        return player
    }
}
