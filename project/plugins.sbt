addSbtPlugin("com.dwijnand"      % "sbt-dynver"      % "3.0.0")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"    % "1.5.1")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.0.0")
addSbtPlugin("org.wartremover"   % "sbt-wartremover" % "2.2.1")
addSbtPlugin("io.get-coursier"   % "sbt-coursier"    % "1.0.3")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"       % "1.0.0-RC1")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25" // Needed by sbt-git
