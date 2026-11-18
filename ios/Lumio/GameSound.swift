import AVFAudio

enum GameCue: String {
    case correct, incorrect, timeout, swap, completion
}

@MainActor
final class GameSound {
    private var players: [GameCue: AVAudioPlayer] = [:]

    func play(_ cue: GameCue) {
        let enabled = UserDefaults.standard.object(forKey: SettingKey.gameSound) as? Bool ?? true
        guard enabled else { return }
        do {
            // Ambient mixes with other audio and respects the Ring/Silent switch.
            try AVAudioSession.sharedInstance().setCategory(.ambient)
            if players[cue] == nil,
               let url = Bundle.main.url(forResource: "cue-\(cue.rawValue)", withExtension: "wav") {
                players[cue] = try AVAudioPlayer(contentsOf: url)
            }
            guard let player = players[cue] else { return }
            player.currentTime = 0
            player.play()
        } catch {
            // Audio is optional. Game transitions always finish.
        }
    }
}
